package com.lin.elm.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 通用缓存组件：把「缓存穿透 / 击穿 / 雪崩」三种问题的处理封装在一处，业务代码只调一行
 *
 * ① 缓存穿透（查不存在的数据，请求全打到数据库）
 *    → 查库结果为空时，往 Redis 写一个 __NULL__ 占位符，TTL 短（60 秒），后续同样的查询直接命中占位符
 * ② 缓存击穿（热点 key 过期瞬间，大量请求同时打到数据库）
 *    → 用 Redis SETNX 做分布式锁，只让一个线程去查库并回填，其它线程等一会儿读缓存
 * ③ 缓存雪崩（大批 key 同时过期，数据库被瞬间压垮）
 *    → TTL = 基础时间 + 随机偏移，让过期时间散开
 *
 * 另外：锁的释放用 Lua 脚本「判断归属再删」，避免误删别人的锁
 */
@Service
public class CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);

    /** 空值占位符：防穿透 */
    private static final String NULL_MARKER = "__NULL__";
    /** 正常数据的过期时间：5 分钟 + 0~60 秒随机 */
    private static final long BASE_TTL_SECONDS = 300;
    private static final int RANDOM_TTL_SECONDS = 60;
    /** 空值的过期时间：只挡 1 分钟，避免"新增了数据却一直查不到" */
    private static final long NULL_TTL_SECONDS = 60;
    /** 分布式锁的过期时间：防止业务异常后锁一直不释放 */
    private static final long LOCK_TTL_SECONDS = 10;

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private DefaultRedisScript<Long> unlockScript;

    /** 带随机偏移的 TTL：防雪崩 */
    private long randomTtl() {
        return BASE_TTL_SECONDS + ThreadLocalRandom.current().nextLong(RANDOM_TTL_SECONDS);
    }

    /**
     * 读列表缓存（商家列表、菜品列表这类）
     * 外层统一兜底：Redis 挂了也不能影响业务，直接查库返回
     */
    public <T> List<T> getList(String key, TypeReference<List<T>> typeRef, Supplier<List<T>> loader) {
        try {
            return getListInternal(key, typeRef, loader);
        } catch (Exception e) {
            log.warn("[缓存] Redis 异常，降级为直接查库：{}（{}）", key, e.getMessage());
            return loader.get();
        }
    }

    private <T> List<T> getListInternal(String key, TypeReference<List<T>> typeRef, Supplier<List<T>> loader) {
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            if (NULL_MARKER.equals(cached)) {
                log.info("[缓存] 命中空值，直接返回（防穿透）：{}", key);
                return new ArrayList<>();
            }
            log.info("[缓存] 命中：{}（不查数据库）", key);
            return parseList(cached, typeRef);
        }

        String lockKey = "lock:" + key;
        String lockValue = UUID.randomUUID().toString();
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, LOCK_TTL_SECONDS, TimeUnit.SECONDS);

        if (!Boolean.TRUE.equals(locked)) {
            // 没抢到锁：说明有线程正在查库回填，稍等一下再读缓存（防击穿关键）
            sleep(50);
            String retry = redisTemplate.opsForValue().get(key);
            if (retry != null) {
                log.info("[缓存] 等到了别人回填的缓存：{}", key);
                return NULL_MARKER.equals(retry) ? new ArrayList<>() : parseList(retry, typeRef);
            }
            log.info("[缓存] 未抢到锁且缓存仍为空，直接查库兜底：{}", key);
            return loader.get();
        }

        try {
            // 双重检查：可能刚才别的线程已经回填过了
            String again = redisTemplate.opsForValue().get(key);
            if (again != null && !NULL_MARKER.equals(again)) {
                return parseList(again, typeRef);
            }

            List<T> data = loader.get();
            if (data == null || data.isEmpty()) {
                redisTemplate.opsForValue().set(key, NULL_MARKER, NULL_TTL_SECONDS, TimeUnit.SECONDS);
                log.info("[缓存] 数据库也没有数据，写入空值占位符 {} 秒（防穿透）：{}", NULL_TTL_SECONDS, key);
                return data == null ? new ArrayList<>() : data;
            }
            long ttl = randomTtl();
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(data), ttl, TimeUnit.SECONDS);
            log.info("[缓存] 查库并回填缓存，TTL={} 秒（含随机偏移，防雪崩）：{}", ttl, key);
            return data;
        } catch (Exception e) {
            // 缓存出问题不能影响业务：兜底直接查库
            log.error("写缓存失败，直接查库兜底：{}", key, e);
            return loader.get();
        } finally {
            unlock(lockKey, lockValue);
        }
    }

    /**
     * 读对象缓存（商家详情、菜品详情这类）
     * 同样：Redis 挂了直接查库，不影响业务
     */
    public <T> T getObject(String key, Class<T> clazz, Supplier<T> loader) {
        try {
            return getObjectInternal(key, clazz, loader);
        } catch (Exception e) {
            log.warn("[缓存] Redis 异常，降级为直接查库：{}（{}）", key, e.getMessage());
            return loader.get();
        }
    }

    private <T> T getObjectInternal(String key, Class<T> clazz, Supplier<T> loader) {
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            if (NULL_MARKER.equals(cached)) {
                log.info("[缓存] 命中空值，直接返回（防穿透）：{}", key);
                return null;
            }
            log.info("[缓存] 命中：{}（不查数据库）", key);
            return parseObject(cached, clazz);
        }

        String lockKey = "lock:" + key;
        String lockValue = UUID.randomUUID().toString();
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, LOCK_TTL_SECONDS, TimeUnit.SECONDS);

        if (!Boolean.TRUE.equals(locked)) {
            sleep(50);
            String retry = redisTemplate.opsForValue().get(key);
            if (retry != null) {
                return NULL_MARKER.equals(retry) ? null : parseObject(retry, clazz);
            }
            return loader.get();
        }

        try {
            String again = redisTemplate.opsForValue().get(key);
            if (again != null && !NULL_MARKER.equals(again)) {
                return parseObject(again, clazz);
            }
            T data = loader.get();
            if (data == null) {
                redisTemplate.opsForValue().set(key, NULL_MARKER, NULL_TTL_SECONDS, TimeUnit.SECONDS);
                log.info("[缓存] 数据不存在，写入空值占位符（防穿透）：{}", key);
                return null;
            }
            long ttl = randomTtl();
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(data), ttl, TimeUnit.SECONDS);
            log.info("[缓存] 查库并回填缓存，TTL={} 秒：{}", ttl, key);
            return data;
        } catch (Exception e) {
            log.error("写缓存失败，直接查库兜底：{}", key, e);
            return loader.get();
        } finally {
            unlock(lockKey, lockValue);
        }
    }

    /** 删除缓存（数据变更时调用，保证一致性） */
    public void evict(String... keys) {
        if (keys == null || keys.length == 0) {
            return;
        }
        Long removed = redisTemplate.delete(Arrays.asList(keys));
        log.info("[缓存] 已删除 {} 个 key：{}", removed, Arrays.toString(keys));
    }

    public StringRedisTemplate getRedisTemplate() {
        return redisTemplate;
    }

    private void unlock(String lockKey, String lockValue) {
        try {
            redisTemplate.execute(unlockScript, Collections.singletonList(lockKey), lockValue);
        } catch (Exception e) {
            log.warn("释放锁失败（锁会自动过期）：{}", lockKey, e);
        }
    }

    private <T> List<T> parseList(String json, TypeReference<List<T>> typeRef) {
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            log.error("缓存反序列化失败，按未命中处理：{}", json, e);
            return null;
        }
    }

    private <T> T parseObject(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            log.error("缓存反序列化失败，按未命中处理：{}", json, e);
            return null;
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

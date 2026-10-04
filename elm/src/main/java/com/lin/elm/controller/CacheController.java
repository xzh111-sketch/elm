package com.lin.elm.controller;

import com.lin.elm.conf.CacheKeys;
import com.lin.elm.service.CacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 缓存管理接口（演示/运维用）
 *
 * 真实项目里缓存的一致性靠"写数据时删缓存"，本项目没有商家/菜品的后台管理功能，
 * 所以提供一个手动清理入口，改完数据库可以直接清缓存
 */
@RestController
@RequestMapping("/api/cache")
public class CacheController {

    @Autowired
    private CacheService cacheService;

    /** 清空本项目所有缓存 */
    @PostMapping("/clear")
    public int clear() {
        Set<String> keys = cacheService.getRedisTemplate().keys(CacheKeys.PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return 0;
        }
        Long removed = cacheService.getRedisTemplate().delete(keys);
        return removed == null ? 0 : removed.intValue();
    }

    /**
     * 查看当前所有缓存 key 以及剩余过期时间
     * 用途：能直观看到「每个 key 的 TTL 是散开的」——这就是防雪崩的效果
     *
     * 注意：这里用了 KEYS 命令，生产环境要用 SCAN（KEYS 会阻塞 Redis）
     */
    @GetMapping("/keys")
    public Map<String, Object> keys() {
        Map<String, Object> result = new LinkedHashMap<>();
        Set<String> keys = cacheService.getRedisTemplate().keys(CacheKeys.PREFIX + "*");
        if (keys == null) {
            return result;
        }
        for (String key : keys) {
            Long ttl = cacheService.getRedisTemplate().getExpire(key, TimeUnit.SECONDS);
            result.put(key, ttl);
        }
        return result;
    }
}

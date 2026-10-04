-- 秒杀核心脚本：把「判断是否重复购买」和「扣减库存」合并成一次原子操作
-- 为什么必须用 Lua：这两步如果分开执行，在并发下会出现
--   ① 同一个用户连点两次都通过判断 → 重复下单
--   ② 库存扣成负数 → 超卖
-- Redis 执行 Lua 是单线程原子的，中间不会被其它请求插进来
--
-- KEYS[1] = 库存 key          seckill:stock:{seckillId}
-- KEYS[2] = 已购用户集合 key   seckill:users:{seckillId}
-- ARGV[1] = userId
-- 返回值：1 = 抢购成功；0 = 库存不足；-1 = 重复抢购

if redis.call('sismember', KEYS[2], ARGV[1]) == 1 then
    return -1
end

local stock = tonumber(redis.call('get', KEYS[1]) or '-1')
if stock <= 0 then
    return 0
end

redis.call('decr', KEYS[1])
redis.call('sadd', KEYS[2], ARGV[1])
return 1

-- 分布式锁解锁脚本：只删除"自己加的锁"
-- 为什么不能直接 del：如果线程 A 的业务执行超过了锁的过期时间，锁会自动释放，
-- 此时线程 B 拿到了锁；A 执行完直接 del 就会把 B 的锁删掉，导致锁形同虚设。
-- KEYS[1] = 锁 key    ARGV[1] = 加锁时写入的唯一值（UUID）
if redis.call('get', KEYS[1]) == ARGV[1] then
    return redis.call('del', KEYS[1])
else
    return 0
end

package com.lin.elm.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;

/**
 * Redis 配置
 * 说明：StringRedisTemplate 是 starter 自动装配好的，这里只需要把秒杀的 Lua 脚本注册成 Bean
 */
@Configuration
public class RedisConfig {

    @Bean
    public DefaultRedisScript<Long> seckillScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("lua/seckill.lua"));
        script.setResultType(Long.class);
        return script;
    }

    /** 分布式锁解锁脚本：判断锁的归属后再删除，避免误删别人的锁 */
    @Bean
    public DefaultRedisScript<Long> unlockScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("lua/unlock.lua"));
        script.setResultType(Long.class);
        return script;
    }
}

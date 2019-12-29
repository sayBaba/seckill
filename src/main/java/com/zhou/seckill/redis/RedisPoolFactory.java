package com.zhou.seckill.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Component
public class RedisPoolFactory {

    @Autowired
    private RedisConfig  redisConfig;

    @Bean
    public JedisPool jedisPoolFactory(){
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxIdle(redisConfig.getPoolMaxIdle());
        config.setMaxTotal(redisConfig.getPoolMaxTotal());
        config.setMaxWaitMillis(redisConfig.getPoolMaxWait() * 1000);
        return new JedisPool(config, redisConfig.getHost(), redisConfig.getPort(),
            redisConfig.getTimeout() * 1000, null, 0);
    }

}
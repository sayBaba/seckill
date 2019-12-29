package com.zhou.seckill.service;

import com.zhou.seckill.redis.KeyPrefix;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson.JSON;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Service
public class RedisService {

  @Autowired
  private JedisPool jedisPool;

  public <T> T get(KeyPrefix prefix, String key, Class<T> clazz) {
    Jedis jedis = null;
    try {
      jedis = jedisPool.getResource();
      String realKey = prefix.getPrefix() + key;
      String str = jedis.get(realKey);
      return stringToBean(str, clazz);
    } finally {
      returnToPool(jedis);
    }
  }

  public <T> boolean set(KeyPrefix prefix, String key, T value) {
    Jedis jedis = null;
    try {
      jedis = jedisPool.getResource();
      String str = beanToString(value);
      if (str == null || str.length() <= 0) {
        return false;
      }
      String realKey = prefix.getPrefix() + key;
      int seconds = prefix.expireSeconds();
      if (seconds <= 0) {
        jedis.set(realKey, str);
      } else {
        jedis.setex(realKey, seconds, str);
      }
      return true;
    } finally {
      returnToPool(jedis);
    }
  }

  public boolean delete(KeyPrefix prefix, String key) {
    Jedis jedis = null;
    try {
      jedis = jedisPool.getResource();
      String realKey = prefix.getPrefix() + key;
      long ret = jedis.del(realKey);
      return ret > 0;
    } finally {
      returnToPool(jedis);
    }
  }

  public boolean exists(KeyPrefix prefix, String key) {
    Jedis jedis = null;
    try {
      jedis = jedisPool.getResource();
      String realKey = prefix.getPrefix() + key;
      return jedis.exists(realKey);
    } finally {
      returnToPool(jedis);
    }
  }

  public Long incr(KeyPrefix prefix, String key) {
    Jedis jedis = null;
    try {
      jedis = jedisPool.getResource();
      String realKey = prefix.getPrefix() + key;
      return jedis.incr(realKey);
    } finally {
      returnToPool(jedis);
    }
  }

  public Long decr(KeyPrefix prefix, String key) {
    Jedis jedis = null;
    try {
      jedis = jedisPool.getResource();
      String realKey = prefix.getPrefix() + key;
      return jedis.decr(realKey);
    } finally {
      returnToPool(jedis);
    }
  }

  public <T> String beanToString(T value) {
    if (value == null) {
      return null;
    }
    return JSON.toJSONString(value);
  }

  public <T> T stringToBean(String str, Class<T> clazz) {
    if (str == null || str.length() <= 0 || clazz == null) {
      return null;
    }
    return JSON.toJavaObject(JSON.parseObject(str), clazz);
  }

  private void returnToPool(Jedis jedis) {
    if (jedis != null) {
      jedis.close();
    }
  }
}

package com.zhou.seckill.redis;

public interface KeyPrefix {

    /**
     * 有效期
     */
    int expireSeconds();

    /**
     * 前缀
     */
    String getPrefix();
}
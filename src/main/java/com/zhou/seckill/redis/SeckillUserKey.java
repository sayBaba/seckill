package com.zhou.seckill.redis;

public class SeckillUserKey extends BasePrefix {

  private static final int TOKEN_EXPIRE = 3600 * 24 * 2;

  private SeckillUserKey(int expireSeconds, String prefix) {
    super(expireSeconds, prefix);
  }

  public static SeckillUserKey token = new SeckillUserKey(TOKEN_EXPIRE, "token");
  public static SeckillUserKey getById = new SeckillUserKey(0, "id");
}

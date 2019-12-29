package com.zhou.seckill.redis;

public class SeckillKey extends BasePrefix {

  private SeckillKey(int expireSeconds, String prefix) {
    super(expireSeconds, prefix);
  }

  public static SeckillKey isGoodsOver = new SeckillKey(0, "go");
  public static SeckillKey getSeckillPath = new SeckillKey(60, "seckillPath");
  public static SeckillKey getSeckillVerifyCode = new SeckillKey(300, "seckillVerifyCode");
}

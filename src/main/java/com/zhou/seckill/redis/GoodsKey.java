package com.zhou.seckill.redis;

public class GoodsKey extends BasePrefix {

  private GoodsKey(int expireSeconds, String prefix) {
    super(expireSeconds, prefix);
  }

  public static GoodsKey getGoodsList = new GoodsKey(60, "goodsList");
  public static GoodsKey getGoodsDetail = new GoodsKey(60, "goodsDetail");
  public static GoodsKey getSeckillGoodsStock = new GoodsKey(0, "seckillGoodsStock");
}

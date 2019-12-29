package com.zhou.seckill.service;

import com.zhou.seckill.domain.OrderInfo;
import com.zhou.seckill.domain.SeckillOrder;
import com.zhou.seckill.domain.SeckillUser;
import com.zhou.seckill.redis.SeckillKey;
import com.zhou.seckill.util.MD5Util;
import com.zhou.seckill.util.UUIDUtil;
import com.zhou.seckill.vo.GoodsVo;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Random;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SeckillService {

  private static char[] ops = new char[]{'+', '-', '*'};

  @Autowired
  GoodsService goodsService;

  @Autowired
  private OrderService orderService;

  @Autowired
  private RedisService redisService;

  @Transactional
  public OrderInfo seckill(SeckillUser user, GoodsVo goods) {
    //减库存 下订单 写入秒杀订单
    boolean success = goodsService.reduceStock(goods);
    if (success) {
      return orderService.createOrder(user, goods);
    } else {
      setGoodsOver(goods.getId());
      return null;
    }
  }

  public long getSeckillResult(Long userId, long goodsId) {
    SeckillOrder order = orderService.getSeckillOrderByUserIdAndGoodsId(userId, goodsId);
    if (order != null) {
      //秒杀成功
      return order.getOrderId();
    } else {
      boolean isOver = getGoodsOver(goodsId);
      if (isOver) {
        return -1;
      }
      return 0;
    }
  }

  public boolean checkPath(SeckillUser user, long goodsId, String path) {
    if (user == null || path == null) {
      return false;
    }
    String pathOld = redisService
        .get(SeckillKey.getSeckillPath, "" + user.getId() + "_" + goodsId, String.class);
    return path.equals(pathOld);
  }

  public String createSeckillPath(SeckillUser user, long goodsId) {
    if (user == null || goodsId <= 0) {
      return null;
    }
    String str = MD5Util.md5(UUIDUtil.uuid());
    redisService.set(SeckillKey.getSeckillPath, "" + user.getId() + "_" + goodsId, str);
    return str;
  }

  public BufferedImage createVerifyCode(SeckillUser user, long goodsId) {
    if (user == null || goodsId <= 0) {
      return null;
    }
    int width = 80, height = 32;
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics g = image.getGraphics();
    g.setColor(new Color(0xDCDCDC));
    g.fillRect(0, 0, width, height);
    g.setColor(Color.black);
    g.drawRect(0, 0, width - 1, height - 1);
    Random rdm = new Random();
    for (int i = 0; i < 50; i++) {
      int x = rdm.nextInt(width);
      int y = rdm.nextInt(height);
      g.drawOval(x, y, 0, 0);
    }
    String verifyCode = generateVerifyCode(rdm);
    g.setColor(new Color(0, 100, 0));
    g.setFont(new Font("Candara", Font.BOLD, 24));
    g.drawString(verifyCode, 8, 24);
    g.dispose();
    int rnd = calc(verifyCode);
    redisService.set(SeckillKey.getSeckillVerifyCode, user.getId() + "," + goodsId, rnd);
    return image;
  }

  public boolean checkVerifyCode(SeckillUser user, long goodsId, int verifyCode) {
    if (user == null || goodsId <= 0) {
      return false;
    }
    Integer codeOld = redisService
        .get(SeckillKey.getSeckillVerifyCode, user.getId() + "," + goodsId, Integer.class);
    if (codeOld == null || codeOld - verifyCode != 0) {
      return false;
    }
    redisService.delete(SeckillKey.getSeckillVerifyCode, user.getId() + "," + goodsId);
    return true;
  }

  private void setGoodsOver(Long goodsId) {
    redisService.set(SeckillKey.isGoodsOver, String.valueOf(goodsId), true);
  }

  private boolean getGoodsOver(long goodsId) {
    return redisService.exists(SeckillKey.isGoodsOver, String.valueOf(goodsId));
  }

  private static int calc(String exp) {
    try {
      ScriptEngineManager manager = new ScriptEngineManager();
      ScriptEngine engine = manager.getEngineByName("JavaScript");
      return (Integer) engine.eval(exp);
    } catch (Exception e) {
      e.printStackTrace();
      return 0;
    }
  }

  private String generateVerifyCode(Random rdm) {
    int num1 = rdm.nextInt(10);
    int num2 = rdm.nextInt(10);
    int num3 = rdm.nextInt(10);
    char op1 = ops[rdm.nextInt(3)];
    char op2 = ops[rdm.nextInt(3)];
    return "" + num1 + op1 + num2 + op2 + num3;
  }
}

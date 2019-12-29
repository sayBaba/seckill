package com.zhou.seckill.service;

import com.zhou.seckill.dao.OrderDao;
import com.zhou.seckill.domain.OrderInfo;
import com.zhou.seckill.domain.SeckillOrder;
import com.zhou.seckill.domain.SeckillUser;
import com.zhou.seckill.redis.OrderKey;
import com.zhou.seckill.vo.GoodsVo;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

  @Autowired
  private OrderDao orderDao;

  @Autowired
  private RedisService redisService;

  public SeckillOrder getSeckillOrderByUserIdAndGoodsId(long userId, long goodsId) {
    return redisService
        .get(OrderKey.getSeckillOrderByUidAndGid, "" + userId + "_" + goodsId, SeckillOrder.class);
  }

  public OrderInfo getOrderById(long orderId) {
    return orderDao.getOrderById(orderId);
  }

  @Transactional
  public OrderInfo createOrder(SeckillUser user, GoodsVo goods) {
    OrderInfo orderInfo = new OrderInfo();
    orderInfo.setCreateDate(new Date());
    orderInfo.setDeliveryAddrId(0L);
    orderInfo.setGoodsCount(1);
    orderInfo.setGoodsId(goods.getId());
    orderInfo.setGoodsName(goods.getGoodsName());
    orderInfo.setGoodsPrice(goods.getGoodsPrice());
    orderInfo.setOrderChannel(1);
    orderInfo.setStatus(0);
    orderInfo.setUserId(user.getId());
    orderDao.insert(orderInfo);

    SeckillOrder seckillOrder = new SeckillOrder();
    seckillOrder.setGoodsId(goods.getId());
    seckillOrder.setOrderId(orderInfo.getId());
    seckillOrder.setUserId(user.getId());
    orderDao.insertSeckillOrder(seckillOrder);

    redisService.set(OrderKey.getSeckillOrderByUidAndGid, "" + user.getId() + "_" + goods.getId(),
        seckillOrder);
    return orderInfo;
  }
}

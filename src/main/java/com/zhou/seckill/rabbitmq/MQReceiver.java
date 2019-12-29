package com.zhou.seckill.rabbitmq;

import com.zhou.seckill.domain.SeckillOrder;
import com.zhou.seckill.domain.SeckillUser;
import com.zhou.seckill.service.GoodsService;
import com.zhou.seckill.service.OrderService;
import com.zhou.seckill.service.RedisService;
import com.zhou.seckill.service.SeckillService;
import com.zhou.seckill.vo.GoodsVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MQReceiver {

  private static Logger log = LoggerFactory.getLogger(MQReceiver.class);

  @Autowired
  private RedisService redisService;

  @Autowired
  private GoodsService goodsService;

  @Autowired
  private OrderService orderService;

  @Autowired
  private SeckillService seckillService;

  @RabbitListener(queues = MQConfig.SECKILL_QUEUE)
  public void receive(String message) {
    log.info("receive message:" + message);
    SeckillMessage seckillMessage = redisService.stringToBean(message, SeckillMessage.class);
    SeckillUser user = seckillMessage.getUser();
    long goodsId = seckillMessage.getGoodsId();

    GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
    int stock = goods.getStockCount();
    if (stock <= 0) {
      return;
    }

    //判断是否已经秒杀到了
    //由于已经加唯一索引,可以不作判断
    SeckillOrder order = orderService.getSeckillOrderByUserIdAndGoodsId(user.getId(), goodsId);
    if (order != null) {
      return;
    }
    //减库存 下订单 写入秒杀订单
    seckillService.seckill(user, goods);
  }
}

package com.zhou.seckill.rabbitmq;

import com.zhou.seckill.service.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MQSender {

	private static Logger log = LoggerFactory.getLogger(MQSender.class);
	
	@Autowired
	private AmqpTemplate amqpTemplate ;

	@Autowired
	private RedisService redisService;
	
	public void sendSeckillMessage(SeckillMessage seckillMessage) {
		String msg = redisService.beanToString(seckillMessage);
		log.info("send message:"+msg);
		amqpTemplate.convertAndSend(MQConfig.SECKILL_QUEUE, msg);
	}
}

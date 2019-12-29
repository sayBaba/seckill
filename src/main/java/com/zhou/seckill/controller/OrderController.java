package com.zhou.seckill.controller;

import com.zhou.seckill.domain.OrderInfo;
import com.zhou.seckill.domain.SeckillUser;
import com.zhou.seckill.result.CodeMsg;
import com.zhou.seckill.result.Result;
import com.zhou.seckill.service.GoodsService;
import com.zhou.seckill.service.OrderService;
import com.zhou.seckill.vo.GoodsVo;
import com.zhou.seckill.vo.OrderDetailVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/order")
public class OrderController {

  @Autowired
  private OrderService orderService;

  @Autowired
  private GoodsService goodsService;

  @RequestMapping("/detail")
  @ResponseBody
  public Result<OrderDetailVo> info(SeckillUser user,
      @RequestParam("orderId") long orderId) {
    if (user == null) {
      return Result.error(CodeMsg.SESSION_ERROR);
    }

    OrderInfo order = orderService.getOrderById(orderId);
    if (order == null) {
      return Result.error(CodeMsg.ORDER_NOT_EXIST);
    }

    long goodsId = order.getGoodsId();
    GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
    OrderDetailVo vo = new OrderDetailVo();
    vo.setOrder(order);
    vo.setGoods(goods);
    return Result.success(vo);
  }
}

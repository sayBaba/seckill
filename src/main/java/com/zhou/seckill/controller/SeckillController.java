package com.zhou.seckill.controller;

import com.google.common.util.concurrent.RateLimiter;
import com.zhou.seckill.access.AccessLimit;
import com.zhou.seckill.domain.SeckillOrder;
import com.zhou.seckill.domain.SeckillUser;
import com.zhou.seckill.rabbitmq.MQSender;
import com.zhou.seckill.rabbitmq.SeckillMessage;
import com.zhou.seckill.redis.GoodsKey;
import com.zhou.seckill.result.CodeMsg;
import com.zhou.seckill.result.Result;
import com.zhou.seckill.service.GoodsService;
import com.zhou.seckill.service.OrderService;
import com.zhou.seckill.service.RedisService;
import com.zhou.seckill.service.SeckillService;
import com.zhou.seckill.vo.GoodsVo;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/seckill")
public class SeckillController implements InitializingBean {

  @Autowired
  private SeckillService seckillService;

  @Autowired
  private RedisService redisService;

  @Autowired
  private GoodsService goodsService;

  @Autowired
  private OrderService orderService;

  @Autowired
  private MQSender sender;

  //基于令牌桶算法的限流实现类
  private RateLimiter rateLimiter = RateLimiter.create(10);

  private HashMap<Long, Boolean> localOverMap = new HashMap<Long, Boolean>();

  public void afterPropertiesSet() {
    List<GoodsVo> goodsList = goodsService.listGoodsVo();
    if (goodsList == null) {
      return;
    }
    for (GoodsVo goods : goodsList) {
      redisService.set(GoodsKey.getSeckillGoodsStock, String.valueOf(goods.getId()), goods.getStockCount());
      localOverMap.put(goods.getId(), false);
    }
  }

  @RequestMapping(value = "/{path}/do_seckill", method = RequestMethod.POST)
  @ResponseBody
  public Result<Integer> seckill(Model model, SeckillUser user,
      @RequestParam("goodsId") long goodsId,
      @PathVariable("path") String path) {
    if (!rateLimiter.tryAcquire(1000, TimeUnit.MILLISECONDS)) {
      return  Result.error(CodeMsg.ACCESS_LIMIT_REACHED);
    }

    model.addAttribute("user", user);
    if (user == null) {
      return Result.error(CodeMsg.SESSION_ERROR);
    }

    //验证path
    boolean check = seckillService.checkPath(user, goodsId, path);
    if (!check) {
      return Result.error(CodeMsg.REQUEST_ILLEGAL);
    }

    //内存标记，减少redis访问
    boolean over = localOverMap.get(goodsId);
    if (over) {
      return Result.error(CodeMsg.SECKILL_OVER);
    }

    //预减库存
    long stock = redisService.decr(GoodsKey.getSeckillGoodsStock, String.valueOf(goodsId));
    if (stock < 0) {
      localOverMap.put(goodsId, true);
      return Result.error(CodeMsg.SECKILL_OVER);
    }

    //判断是否已经秒杀到了
    SeckillOrder order = orderService.getSeckillOrderByUserIdAndGoodsId(user.getId(), goodsId);
    if (order != null) {
      return Result.error(CodeMsg.REPEATE_SECKILL);
    }

    //入队
    SeckillMessage seckillMessage = new SeckillMessage();
    seckillMessage.setUser(user);
    seckillMessage.setGoodsId(goodsId);
    sender.sendSeckillMessage(seckillMessage);
    return Result.success(0);//排队中
  }

  /**
   * orderId：成功 -1：秒杀失败 0： 排队中
   */
  @RequestMapping(value = "/result", method = RequestMethod.GET)
  @ResponseBody
  public Result<Long> seckillResult(Model model, SeckillUser user,
      @RequestParam("goodsId") long goodsId) {
    model.addAttribute("user", user);
    if (user == null) {
      return Result.error(CodeMsg.SESSION_ERROR);
    }
    long result = seckillService.getSeckillResult(user.getId(), goodsId);
    return Result.success(result);
  }

  @AccessLimit(seconds = 5, maxCount = 5)
  @RequestMapping(value = "/path", method = RequestMethod.GET)
  @ResponseBody
  public Result<String> getSeckillPath(HttpServletRequest request, SeckillUser user,
      @RequestParam("goodsId") long goodsId,
      @RequestParam(value = "verifyCode", defaultValue = "0") int verifyCode
  ) {
    if (user == null) {
      return Result.error(CodeMsg.SESSION_ERROR);
    }

    boolean check = seckillService.checkVerifyCode(user, goodsId, verifyCode);
    if (!check) {
      return Result.error(CodeMsg.REQUEST_ILLEGAL);
    }

    String path = seckillService.createSeckillPath(user, goodsId);
    return Result.success(path);
  }

  @RequestMapping(value = "/verifyCode", method = RequestMethod.GET)
  @ResponseBody
  public Result<String> getSeckillVerifyCode(HttpServletResponse response, SeckillUser user,
      @RequestParam("goodsId") long goodsId) {
    if (user == null) {
      return Result.error(CodeMsg.SESSION_ERROR);
    }
    try {
      BufferedImage image = seckillService.createVerifyCode(user, goodsId);
      OutputStream out = response.getOutputStream();
      ImageIO.write(image, "JPEG", out);
      out.flush();
      out.close();
      return null;
    } catch (Exception e) {
      e.printStackTrace();
      return Result.error(CodeMsg.SECKILL_FAIL);
    }
  }
}

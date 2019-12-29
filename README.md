# 秒杀系统

本系统是使用SpringBoot开发的高并发限时抢购秒杀系统，除了实现基本的登录、查看商品列表、秒杀、下单等功能，项目中还针对高并发情况实现了系统缓存、降级和限流

## 两次MD5加密

将用户输入的密码和固定Salt通过MD5加密生成第一次加密后的密码，再讲该密码和随机生成的Salt通过MD5进行第二次加密，最后将第二次加密后的密码和第二次的随机Salt存数据库

用户端：PASS=MD5(明文+固定Salt) 
服务端：PASS=MD5(用户输入+随机Salt)

好处：

第一次作用：防止用户明文密码在网络进行传输
第二次作用：防止数据库被盗，避免通过MD5反推出密码，双重保险

前端加密：

```javascript
//salt
var password_salt="1a2b3c4d";

var inputPass = $("#password").val();
var salt = password_salt;
var str = "" + salt.charAt(0) + salt.charAt(2) + inputPass + salt.charAt(5) + salt.charAt(4);
var password = md5(str);
```
后端加密：

```java
/**
   * MD5加密，用于存储到数据库
   */ 
  public static String formPassToDBPass(String formPass, String salt) {
    String str = "" + salt.charAt(0) + salt.charAt(2) + formPass + salt.charAt(5) + salt.charAt(4);
    return DigestUtils.md5Hex(str);
  }
```

## Session共享

验证用户账号密码都正确情况下，通过UUID生成唯一id作为token，再将token作为key、用户信息作为value模拟session存储到redis，同时将token存储到cookie，保存登录状态

好处： 在分布式集群情况下，服务器间需要同步，定时同步各个服务器的session信息，会因为延迟到导致session不一致，使用redis把session数据集中存储起来，解决session不一致问题

## 页面缓存

页面缓存：通过在手动渲染得到的html页面缓存到redis

```java
@RequestMapping(value = "/to_list", produces = "text/html")
@ResponseBody
public String list(HttpServletRequest request, HttpServletResponse response, Model model,
    SeckillUser user) {
  model.addAttribute("user", user);

  String html = redisService.get(GoodsKey.getGoodsList, "", String.class);
  if(!StringUtils.isEmpty(html)) {
    return html;
  }

  List<GoodsVo> goodsList = goodsService.listGoodsVo();
  model.addAttribute("goodsList", goodsList);
  SpringWebContext ctx = new SpringWebContext(request, response,
      request.getServletContext(), request.getLocale(), model.asMap(), applicationContext);
  html = thymeleafViewResolver.getTemplateEngine().process("goods_list", ctx);
  if (!StringUtils.isEmpty(html)) {
    redisService.set(GoodsKey.getGoodsList, "", html);
  }
  return html;
}
```

## 对象缓存

对象缓存：包括对用户信息、商品信息、订单信息和token等数据进行缓存，利用缓存来减少对数据库的访问，大大加快查询速度

## 页面静态化

对商品详情和订单详情进行页面静态化处理，页面是存在html，动态数据是通过接口从服务端获取，实现前后端分离，静态页面无需连接数据库打开速度较动态页面会有明显提高

## 解决超卖

描述：比如某商品的库存为1，此时用户1和用户2并发购买该商品，用户1提交订单后该商品的库存被修改为0，而此时用户2并不知道的情况下提交订单，该商品的库存再次被修改为-1，这就是超卖现象

实现：

对库存更新时，先对库存判断，只有当库存大于0才能更新库存

对用户id和商品id建立一个唯一索引，通过这种约束避免同一用户发同时两个请求秒杀到两件相同商品

实现乐观锁，给商品信息表增加一个version字段，为每一条数据加上版本。每次更新的时候version+1，并且更新时候带上版本号，当提交前版本号等于更新前版本号，说明此时没有被其他线程影响到，正常更新，如果冲突了则不会进行提交更新。当库存是足够的情况下发生乐观锁冲突就进行一定次数的重试。


```java
//乐观锁冲突最大重试次数
private static final int DEFAULT_MAX_RETRIES = 5;

public boolean reduceStock(GoodsVo goods) {
	int numAttempts = 0, ret;
	SeckillGoods sg = new SeckillGoods();
	sg.setGoodsId(goods.getId());
	sg.setVersion(goods.getVersion());
	do {
		numAttempts++;
		sg.setVersion(goodsDao.getVersionByGoodsId(goods.getId()));
		ret = goodsDao.reduceStockByVersion(sg);
		if (ret != 0) {
			break;
		}
	} while (numAttempts < DEFAULT_MAX_RETRIES);
	return ret > 0;
}
```

## Redis预处理

系统初始化,把商品库存加载到redis

收到请求在redis预减库存,库存不足,直接返回

这样的话，抢购开始前，将商品和库存数据同步到redis中，所有的抢购操作都在redis中进行处理，通过Redis预减少库存减少数据库访问

## RabbitMQ异步下单

异步下单：如果有库存，不是直接连接数据库写入，而是对RabbitMQ操作，请求入队,立即返回排队中,类似我们在12306买火车票，并不会马上返回下单成功或者失败，而是显示排队中

客户端正常做轮询，判断是否秒杀成功。 服务端在入队之后，会请求出队,生成订单 减少库存

## 本地标记

新建一个HashMap，把商品id和标记值初始化为false存进去。
在预减库存的时候，如果商品库存小于零，做一个标记true，后续的请求不再去访问redis，直接返回失败，好处是可以减少系统开销

这一步是很大的优化，只要库存减成零，后面的请求无论是100个还是一万个都是直接失败，压力很小        

## 秒杀接口地址的隐藏

秒杀接口地址隐藏：每次点击秒杀按钮，才会生成秒杀地址，之前是不知道秒杀地址的。不是写死的，是从服务端获取，动态拼接而成的地址。（Http协议是明文传输，透明的，前端无法控制恶意用户进行攻击）安全校验还是要放在服务端，禁止掉这些恶意服务。

思路：

1.在进行秒杀之前，先请求一个服务端地址，/getSeckillPath 这个地址，用来获取秒杀地址，传参为 商品id，在服务端生成随机数（MD5）作为pathId存入缓存，（缓存过期时间60s），然后将这个随机数返回给前端.

2.获得该pathid,后 前端在用这个pathid拼接在Url上作为参数，去请求doSeckill服务

3.后端接收到这个pathid 参数，并且与缓存中的pathid 比较。

如果通过比较，进行秒杀逻辑，如果不通过，抛出业务异常，非法请求

该操作：可以为了防止，恶意用户登陆之后，获取token的情况下，通过不断调用秒杀地址接口，来达到刷单的恶意请求。

每次的url都不一样，只有真正点击秒杀按钮，才会根据商品和用户id生成对应的秒杀接口地址。

但是，这种情况仍然不能解决 利用 按键精灵或者 机器人 频繁点击按钮的操作，为了降低点击按钮的次数，以及高并发下，防止多个用户在同一时间内，并发出大量请求，加入数学公式图形验证码等防高并发优化。

## 数学公式验证码

描述：点击秒杀前，先让用户输入数学公式验证码，验证正确才能进行秒杀。

好处：

防止恶意的机器人和爬虫
分散用户的请求

实现：

前端通过把商品id作为参数调用服务端创建验证码接口

服务端根据前端传过来的商品id和用户id生成验证码，并将商品id+用户id作为key，生成的验证码作为value存入redis，同时将生成的验证码输入图片写入imageIO让前端展示

将用户输入的验证码与根据商品id+用户id从redis查询到的验证码对比，相同就返回验证成功，进入秒杀；不同或从redis查询的验证码为空都返回验证失败，刷新验证码重试

## 接口限流防刷

描述：当我们去秒杀一些商品时，此时可能会因为访问量太大而导致系统崩溃，此时要使用限流来进行限制访问量，当达到限流阀值，后续请求会被降级；降级后的处理方案可以是：返回排队页面（高峰期访问太频繁，等一会重试）、错误页等。

方法1:

可以把用户访问这个url的次数存入 redis中
做次数限制

key是 前缀+url路径+用户id

使用拦截器，拦截器中判断次数,并计算次数

实现只写一个注解，就可以对这个url判断
多少秒，多少次数，是否需要登录

方法2:

实现：项目使用RateLimiter来实现限流，RateLimiter是guava提供的基于令牌桶算法的限流实现类，通过调整生成token的速率来限制用户频繁访问秒杀页面，从而达到防止超大流量冲垮系统。（令牌桶算法的原理是系统会以一个恒定的速度往桶里放入令牌，而如果请求需要被处理，则需要先从桶里获取一个令牌，当桶里没有令牌可取时，则拒绝服务）


```java
if (!rateLimiter.tryAcquire(1000, TimeUnit.MILLISECONDS)) {
  return  Result.error(CodeMsg.ACCESS_LIMIT_REACHED);
}
```

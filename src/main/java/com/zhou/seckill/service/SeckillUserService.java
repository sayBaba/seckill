package com.zhou.seckill.service;

import com.zhou.seckill.dao.SeckillUserDao;
import com.zhou.seckill.domain.SeckillUser;
import com.zhou.seckill.exception.GlobalException;
import com.zhou.seckill.redis.SeckillUserKey;
import com.zhou.seckill.result.CodeMsg;
import com.zhou.seckill.util.MD5Util;
import com.zhou.seckill.util.UUIDUtil;
import com.zhou.seckill.vo.LoginVo;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SeckillUserService {

  public static final String COOKIE_NAME_TOKEN = "cookie_token";

  @Autowired
  private SeckillUserDao seckillUserDao;

  @Autowired
  private RedisService redisService;

  public SeckillUser getById(long id) {
    SeckillUser user = redisService.get(SeckillUserKey.getById, "" + id, SeckillUser.class);
    if (user != null) {
      return user;
    }

    user = seckillUserDao.getById(id);
    if (user != null) {
      redisService.set(SeckillUserKey.getById, "" + id, user);
    }
    return user;
  }

  public SeckillUser getByToken(HttpServletResponse response, String token) {
    if (StringUtils.isEmpty(token)) {
      return null;
    }
    SeckillUser user = redisService.get(SeckillUserKey.token, token, SeckillUser.class);
    //延长有效期
    if (user != null) {
      addCookie(response, token, user);
    }
    return user;
  }

  public String login(HttpServletResponse response, LoginVo loginVo) {
    if (loginVo == null) {
      throw new GlobalException(CodeMsg.SERVER_ERROR);
    }

    //判断手机号是否存在
    String mobile = loginVo.getMobile();
    SeckillUser user = getById(Long.parseLong(mobile));
    if (user == null) {
      throw new GlobalException(CodeMsg.MOBILE_NOT_EXIST);
    }

    //验证密码
    String formPass = loginVo.getPassword();
    String dbPass = user.getPassword();
    String saltDB = user.getSalt();
    String calcPass = MD5Util.formPassToDBPass(formPass, saltDB);
    if (!calcPass.equals(dbPass)) {
      throw new GlobalException(CodeMsg.PASSWORD_ERROR);
    }

    //生成cookie
    String token = UUIDUtil.uuid();
    addCookie(response, token, user);
    return token;
  }

  private void addCookie(HttpServletResponse response, String token, SeckillUser user) {
    redisService.set(SeckillUserKey.token, token, user);
    Cookie cookie = new Cookie(COOKIE_NAME_TOKEN, token);
    cookie.setMaxAge(SeckillUserKey.token.expireSeconds());
    cookie.setPath("/");
    response.addCookie(cookie);
  }
}

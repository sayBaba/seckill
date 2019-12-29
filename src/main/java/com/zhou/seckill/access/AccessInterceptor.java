package com.zhou.seckill.access;

import com.zhou.seckill.domain.SeckillUser;
import com.zhou.seckill.redis.AccessKey;
import com.zhou.seckill.result.CodeMsg;
import com.zhou.seckill.result.Result;
import com.zhou.seckill.service.RedisService;
import com.zhou.seckill.service.SeckillUserService;
import java.io.OutputStream;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.alibaba.fastjson.JSON;

@Service
public class AccessInterceptor extends HandlerInterceptorAdapter {

  @Autowired
  private SeckillUserService userService;

  @Autowired
  private RedisService redisService;

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    if (handler instanceof HandlerMethod) {
      SeckillUser user = getUser(request, response);
      UserContext.setUser(user);

      HandlerMethod handlerMethod = (HandlerMethod) handler;
      AccessLimit accessLimit = handlerMethod.getMethodAnnotation(AccessLimit.class);
      if (accessLimit == null) {
        return true;
      }

      int seconds = accessLimit.seconds();
      int maxCount = accessLimit.maxCount();
      boolean needLogin = accessLimit.needLogin();
      String key = request.getRequestURI();
      if (needLogin) {
        if (user == null) {
          render(response, CodeMsg.SESSION_ERROR);
          return false;
        }
        key += "_" + user.getId();
      }

      AccessKey accessKey = AccessKey.withExpire(seconds);
      Integer count = redisService.get(accessKey, key, Integer.class);
      if (count == null) {
        redisService.set(accessKey, key, 1);
      } else if (count < maxCount) {
        redisService.incr(accessKey, key);
      } else {
        render(response, CodeMsg.ACCESS_LIMIT_REACHED);
        return false;
      }
    }
    return true;
  }

  private void render(HttpServletResponse response, CodeMsg cm) throws Exception {
    response.setContentType("application/json;charset=UTF-8");
    OutputStream out = response.getOutputStream();
    String str = JSON.toJSONString(Result.error(cm));
    out.write(str.getBytes("UTF-8"));
    out.flush();
    out.close();
  }

  private SeckillUser getUser(HttpServletRequest request, HttpServletResponse response) {
    String paramToken = request.getParameter(SeckillUserService.COOKIE_NAME_TOKEN);
    String cookieToken = getCookieValue(request, SeckillUserService.COOKIE_NAME_TOKEN);
    if (StringUtils.isEmpty(cookieToken) && StringUtils.isEmpty(paramToken)) {
      return null;
    }
    String token = StringUtils.isEmpty(paramToken) ? cookieToken : paramToken;
    return userService.getByToken(response, token);
  }

  private String getCookieValue(HttpServletRequest request, String cookieName) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null || cookies.length <= 0) {
      return null;
    }
    for (Cookie cookie : cookies) {
      if (cookie.getName().equals(cookieName)) {
        return cookie.getValue();
      }
    }
    return null;
  }
}
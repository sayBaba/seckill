package com.zhou.seckill.config;

import com.zhou.seckill.access.UserContext;
import com.zhou.seckill.domain.SeckillUser;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Service
public class UserArgumentResolver implements HandlerMethodArgumentResolver {

  public boolean supportsParameter(MethodParameter parameter) {
    Class<?> clazz = parameter.getParameterType();
    return clazz == SeckillUser.class;
  }

  public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
    return UserContext.getUser();
  }
}

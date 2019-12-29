package com.zhou.seckill.util;

import org.apache.commons.codec.digest.DigestUtils;

public class MD5Util {

  public static String md5(String src) {
    return DigestUtils.md5Hex(src);
  }

  /**
   * MD5加密，用于存储到数据库
   */
  public static String formPassToDBPass(String formPass, String salt) {
    String str = "" + salt.charAt(0) + salt.charAt(2) + formPass + salt.charAt(5) + salt.charAt(4);
    return md5(str);
  }
}
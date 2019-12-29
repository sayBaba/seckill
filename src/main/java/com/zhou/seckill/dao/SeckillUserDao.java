package com.zhou.seckill.dao;

import com.zhou.seckill.domain.SeckillUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SeckillUserDao {

  @Select("select * from t_seckill_user where id = #{id}")
  SeckillUser getById(@Param("id") long id);

  @Update("update t_seckill_user set password = #{password} where id = #{id}")
  void update(SeckillUser toBeUpdate);
}

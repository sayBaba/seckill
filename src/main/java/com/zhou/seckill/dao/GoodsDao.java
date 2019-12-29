package com.zhou.seckill.dao;

import com.zhou.seckill.domain.SeckillGoods;
import com.zhou.seckill.vo.GoodsVo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface GoodsDao {

  @Select("select g.*,mg.stock_count, mg.start_date, mg.end_date,mg.seckill_price from t_seckill_goods mg left join goods g on mg.goods_id = g.id")
  List<GoodsVo> listGoodsVo();

  @Select("select g.*,mg.stock_count, mg.start_date, mg.end_date,mg.seckill_price from seckill_goods mg left join goods g on mg.goods_id = g.id where g.id = #{goodsId}")
  GoodsVo getGoodsVoByGoodsId(@Param("goodsId")long goodsId);

  @Update("update t_goods_seckill set stock_count = stock_count - 1, version= version + 1 where goods_id = #{goodsId} and stock_count > 0 and version = #{version}")
  int reduceStockByVersion(SeckillGoods seckillGoods);

  @Select("select version from t_goods_seckill  where goods_id = #{goodsId}")
  int getVersionByGoodsId(@Param("goodsId") long goodsId);
}

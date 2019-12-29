package com.zhou.seckill.service;

import com.zhou.seckill.dao.GoodsDao;
import com.zhou.seckill.domain.SeckillGoods;
import com.zhou.seckill.vo.GoodsVo;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GoodsService {

	//乐观锁冲突最大重试次数
	private static final int DEFAULT_MAX_RETRIES = 5;
	
	@Autowired
	private GoodsDao goodsDao;
	
	public List<GoodsVo> listGoodsVo(){
		return goodsDao.listGoodsVo();
	}

	public GoodsVo getGoodsVoByGoodsId(long goodsId) {
		return goodsDao.getGoodsVoByGoodsId(goodsId);
	}

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
}

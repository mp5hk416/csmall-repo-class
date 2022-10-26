package cn.tedu.mall.seckill.mapper;

import cn.tedu.mall.pojo.seckill.model.SeckillSku;
import cn.tedu.mall.pojo.seckill.model.SeckillSpu;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SeckillSpuMapper {
    //查詢秒殺商品列表
    List<SeckillSpu> findSeckillSpus();
    //根據給定的時間查詢秒殺商品信息
    List<SeckillSpu> findSeckillSpusByTime(LocalDateTime time);
    //查詢所有秒殺商品spu的id數組----對布隆過濾器做準備(根據id值判斷是否要往後面走)
    Long[] findAllSeckillSpuIds();
    //根據spuid查詢seckillspu信息
    SeckillSpu findAllSeckillSpuId(Long id);

}

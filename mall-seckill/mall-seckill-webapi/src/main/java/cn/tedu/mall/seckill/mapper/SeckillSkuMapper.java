package cn.tedu.mall.seckill.mapper;

import cn.tedu.mall.pojo.seckill.model.SeckillSku;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeckillSkuMapper {
    //根據spuid查詢秒殺sku列表
    List<SeckillSku> findSeckillSkusBySpuId(Long spuId);

    //根據skuId修改庫存數
    void updateReduceStockBySkuId(@Param("skuId") Long skuId,@Param("quantity") Integer quantity);
}

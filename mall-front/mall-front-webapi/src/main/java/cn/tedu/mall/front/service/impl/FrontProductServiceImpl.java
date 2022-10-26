package cn.tedu.mall.front.service.impl;

import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.front.service.IFrontProductService;
import cn.tedu.mall.pojo.product.vo.*;
import cn.tedu.mall.product.service.front.IForFrontAttributeService;
import cn.tedu.mall.product.service.front.IForFrontSkuService;
import cn.tedu.mall.product.service.front.IForFrontSpuService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Hi 俊翔
 * 現在是下午 07:44 2022/9/21 2022
 * 好好加油吧。
 * 不能再浪費時間了
 * 規劃自己，超越自己
 */
@Service
@DubboService
@Slf4j
public class FrontProductServiceImpl implements IFrontProductService {

    @DubboReference
    private IForFrontSpuService dubboSpuService;

    //聲明消費sku相關業務邏輯
    @DubboReference
    private IForFrontSkuService dubboSkuService;

    //聲明消費商品餐數的選項(attribute)的業務邏輯
    @DubboReference
    private IForFrontAttributeService dubboAttributeService;


    @Override
    public JsonPage<SpuListItemVO> listSpuByCategoryId(Long categoryId, Integer page, Integer pageSize) {
        //IForFrontSpuService的實現類中完成的就是分頁查詢
        JsonPage<SpuListItemVO> list = dubboSpuService.listSpuByCategoryId(categoryId,page,pageSize);
        return list;
    }
    //根據spuId查詢spu信息
    @Override
    public SpuStandardVO getFrontSpuById(Long id) {
        //Dubboˇ調用spuService方法
        SpuStandardVO standardVO = dubboSpuService.getSpuById(id);
        return standardVO;
    }

    //根據spuid查詢sku列表
    @Override
    public List<SkuStandardVO> getFrontSkusBySpuId(Long spuId) {

        List<SkuStandardVO> list = dubboSkuService.getSkusBySpuId(spuId);
        return list;
    }
    //根據spuid查詢spuDetail信息
    @Override
    public SpuDetailStandardVO getSpuDetail(Long spuId) {
        SpuDetailStandardVO spuDetailById = dubboSpuService.getSpuDetailById(spuId);

        return spuDetailById;
    }
    //根據spuid查詢參數選項的思路
    @Override
    public List<AttributeStandardVO> getSpuAttributesBySpuId(Long spuId) {
        List<AttributeStandardVO> list = dubboAttributeService.getSpuAttributesBySpuId(spuId);
        return list;
    }
}

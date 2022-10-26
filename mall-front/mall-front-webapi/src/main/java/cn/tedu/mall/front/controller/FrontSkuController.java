package cn.tedu.mall.front.controller;

import cn.tedu.mall.common.restful.JsonResult;
import cn.tedu.mall.front.service.IFrontProductService;
import cn.tedu.mall.pojo.product.vo.SkuStandardVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Hi 俊翔
 * 現在是下午 09:16 2022/9/21 2022
 * 好好加油吧。
 * 不能再浪費時間了
 * 規劃自己，超越自己
 */
@RestController
@RequestMapping("/front/sku")
@Api(tags="前台商品sku模塊")
public class FrontSkuController {

    @Autowired
    private IFrontProductService iFrontProductService;

    @GetMapping("/{spuId}")
    @ApiOperation("根據spuId查詢sku列表")
    @ApiImplicitParam(value="spuId",name="spuId",example="1",required = true,dataType = "long")
    public JsonResult<List<SkuStandardVO>> getSkuListById(@PathVariable long spuId){
        List<SkuStandardVO> list = iFrontProductService.getFrontSkusBySpuId(spuId);
        return JsonResult.ok(list);
    }
}

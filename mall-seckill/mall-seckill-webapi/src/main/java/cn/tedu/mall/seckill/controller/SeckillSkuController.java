package cn.tedu.mall.seckill.controller;

import cn.tedu.mall.common.restful.JsonResult;
import cn.tedu.mall.pojo.seckill.vo.SeckillSkuVO;
import cn.tedu.mall.seckill.service.ISeckillSkuService;
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
 * 時間:2022/10/15  下午 03:15
 * 俊翔，好好加油，找到好工作
 * 項目名:csmall-repo-class
 */

@RestController
@RequestMapping("/seckill/sku")
@Api(tags="秒殺sku模塊")
public class SeckillSkuController {

    @Autowired
    private ISeckillSkuService seckillSkuService;

    @GetMapping("/list/{spuId}")
    @ApiOperation("根據spuid查詢參數")
    @ApiImplicitParam(value="spuid",name="spuId",required = true,dataType = "long")
    public JsonResult<List<SeckillSkuVO>> list(@PathVariable Long spuId){
        List<SeckillSkuVO> list = seckillSkuService.listSeckillSkus(spuId);
        return JsonResult.ok(list);
    }

}

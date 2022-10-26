package cn.tedu.mall.seckill.controller;

import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.common.restful.JsonResult;
import cn.tedu.mall.pojo.seckill.vo.SeckillSpuDetailSimpleVO;
import cn.tedu.mall.pojo.seckill.vo.SeckillSpuVO;
import cn.tedu.mall.seckill.sevice.SeckillSpuServiceImpl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import jdk.nashorn.internal.objects.annotations.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 時間:2022/10/14  下午 08:44
 * 俊翔，好好加油，找到好工作
 * 項目名:csmall-repo-class
 */

@RestController
@Api(tags = "秒殺商品模塊")
@RequestMapping("/seckillspu")
public class SeckillSpuController {

    @Autowired
    private SeckillSpuServiceImpl seckillSpuServiceImpl;

    @ApiOperation("分頁查詢秒殺商品")
    @GetMapping("/list")
    @ApiImplicitParams({
            @ApiImplicitParam(value="頁碼",name="page",required = true,dataType = "int"),
            @ApiImplicitParam(value="每頁調數",name="pageSize",required = true,dataType = "int")
    })
    @PreAuthorize("hasRole('user')")
    public JsonResult<JsonPage<SeckillSpuVO>> listSeckillSpu(Integer page, Integer pageSize){
        JsonPage<SeckillSpuVO> seckillSpuVOJsonPage = seckillSpuServiceImpl.listSeckillSpus(page, pageSize);
        return JsonResult.ok(seckillSpuVOJsonPage);
    }

    @GetMapping("/{spuId}/detail")
    @ApiOperation("根據spuid查詢detail信息")
    @ApiImplicitParam(value="spuid",name="spuId",required = true,dataType = "long")
    public JsonResult<SeckillSpuDetailSimpleVO> getseckillDetail(@PathVariable Long spuId){
        SeckillSpuDetailSimpleVO seckillSpuDetailSimpleVO = seckillSpuServiceImpl.getSeckillSpuDetail(spuId);
        return JsonResult.ok(seckillSpuDetailSimpleVO);
    }

    @GetMapping("/{spuId}")
    @ApiOperation("根據spuid查詢秒殺spu詳情")
    @ApiImplicitParam(value="spuid",name="spuId",required = true,dataType = "long",example = "2" )
    public JsonResult<SeckillSpuDetailSimpleVO> getSeckillSpuVO(@PathVariable Long spuId){
        SeckillSpuDetailSimpleVO seckillSpuDetail = seckillSpuServiceImpl.getSeckillSpuDetail(spuId);
        return JsonResult.ok(seckillSpuDetail);
    }

}

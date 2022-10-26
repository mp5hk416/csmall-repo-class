package cn.tedu.mall.front.controller;

import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.common.restful.JsonResult;
import cn.tedu.mall.front.service.IFrontProductService;
import cn.tedu.mall.pojo.product.vo.AttributeStandardVO;
import cn.tedu.mall.pojo.product.vo.SpuListItemVO;
import cn.tedu.mall.pojo.product.vo.SpuStandardVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import jdk.nashorn.internal.objects.annotations.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Hi 俊翔
 * 現在是下午 07:50 2022/9/21 2022
 * 好好加油吧。
 * 不能再浪費時間了
 * 規劃自己，超越自己
 */

@RestController
@RequestMapping("/front/spu")
@Api(tags="前台商品spu模塊")
public class FrontSpuController {

    @Autowired
    private IFrontProductService frontProductService;

    @GetMapping("/list/{categoryId}")
    @ApiOperation("根據分類id查詢spu列表")
    @ApiImplicitParams({
            @ApiImplicitParam(value="分類id",name="categoryId",example="3",required = true,dataType = "long"),
            @ApiImplicitParam(value="頁碼",name="page",example="1",required = true,dataType = "int"),
            @ApiImplicitParam(value="每條頁數",name="pageSize",example="2",required = true,dataType = "int")
    })
    public JsonResult<JsonPage<SpuListItemVO>> listSpuByPage(@PathVariable Long categoryId,Integer page,Integer pageSize){
        JsonPage<SpuListItemVO> JsonPage = frontProductService.listSpuByCategoryId(categoryId,page,pageSize);
        return JsonResult.ok(JsonPage);
    }

    //根據spuId查詢spu信息
    @GetMapping("/{id}")
    @ApiOperation("根據分類supid查詢信息")
    @ApiImplicitParam(value = "supId",name="id",example="1",required = true,dataType = "long")
    public JsonResult<List<AttributeStandardVO>> getAttributeById(@PathVariable Long id){
        List<AttributeStandardVO> list = frontProductService.getSpuAttributesBySpuId(id);
        return JsonResult.ok(list);

    }


}

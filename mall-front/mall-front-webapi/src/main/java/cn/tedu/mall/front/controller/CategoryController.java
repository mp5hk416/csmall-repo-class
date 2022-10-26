package cn.tedu.mall.front.controller;

import cn.tedu.mall.common.restful.JsonResult;
import cn.tedu.mall.front.service.IFrontCategoryService;
import cn.tedu.mall.pojo.front.entity.FrontCategoryEntity;
import cn.tedu.mall.pojo.front.vo.FrontCategoryTreeVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hi 俊翔
 * 現在是下午 07:46 2022/9/19 2022
 * 好好加油吧。
 * 不能再浪費時間了
 * 規劃自己，超越自己
 */
@RestController
@RequestMapping("/front/category")
@Api(tags = "前台分類查詢")
public class CategoryController {

    @Autowired
    private IFrontCategoryService frontCategoryService;

    @GetMapping("/all")
    @ApiOperation("三級分類樹")
    public JsonResult<FrontCategoryTreeVO<FrontCategoryEntity>> getTreeVO(){
        FrontCategoryTreeVO<FrontCategoryEntity> frontCategoryTreeVO = frontCategoryService.categoryTree();
        return JsonResult.ok(frontCategoryTreeVO);
    }

}

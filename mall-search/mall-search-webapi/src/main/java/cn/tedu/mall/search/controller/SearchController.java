package cn.tedu.mall.search.controller;

import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.common.restful.JsonResult;
import cn.tedu.mall.pojo.search.entity.SpuForElastic;
import cn.tedu.mall.search.service.ISearchService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 時間:2022/10/10  下午 08:05
 * 俊翔，好好加油，找到好工作
 * 項目名:csmall-repo-class
 */

@RestController
@RequestMapping("/search")//盡量減短路徑名稱在搜索功能上
@Api(tags="搜索模塊")

public class SearchController {
    @Autowired
    private ISearchService searchService;

    //搜索模塊的核心就是搜索，方便使用者使用 如:/search
    @GetMapping
    @ApiOperation("根據用戶輸入關鍵字輸入商品")
    @ApiImplicitParams({
            @ApiImplicitParam(value="搜索的關鍵字",name="keyword",dataType = "string"),
            @ApiImplicitParam(value="頁碼",name="page",dataType = "int"),
            @ApiImplicitParam(value="每頁條數",name="pageSize",dataType = "int")
    })
    public JsonResult<JsonPage<SpuForElastic>> searchByKeyword(String keyword, Integer page, Integer pageSize){
        JsonPage<SpuForElastic> list = searchService.search(keyword, page, pageSize);
        return JsonResult.ok(list);
    }

}

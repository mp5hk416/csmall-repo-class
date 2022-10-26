package cn.tedu.mall.order.controller;

import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.common.restful.JsonResult;
import cn.tedu.mall.order.service.IOmsCartService;
import cn.tedu.mall.order.utils.WebConsts;
import cn.tedu.mall.pojo.order.dto.CartAddDTO;
import cn.tedu.mall.pojo.order.dto.CartUpdateDTO;
import cn.tedu.mall.pojo.order.vo.CartStandardVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Hi 俊翔
 * 現在是上午 09:37 2022/9/24 2022
 * 好好加油吧。
 * 不能再浪費時間了
 * 規劃自己，超越自己
 */
@RestController
@RequestMapping("/oms/cart")
@Api(tags="購物車管理模塊")
public class OmsCartController {

    @Autowired
    private IOmsCartService omsCartService;

    @PostMapping("/add")
    @ApiOperation("新增購物車信息")
    //判斷當前用戶是否登入，並具備普通用戶權限ROLE_user
    //訪問前台的普通用戶，在SSO服務器登入獲取JWT時，就已經在全縣列表中添加權限
    @PreAuthorize("hasAuthority ('ROLE_user')")
    //參數cartADDDTO類似的，是需要經過springvalidation框架驗證
    //@Validated 註解能夠在控制層方法編寫，並激化對應的類型驗證過程
    //如果驗證不通過，會由我們編寫的統一異常處裡類BindException
    public JsonResult addCart(@Validated CartAddDTO cartAddDTO){
        omsCartService.addCart(cartAddDTO);
        return JsonResult.ok("增加購物車成功");
    }

    @GetMapping("/list")
    @ApiOperation("分頁用戶查詢sku商品信息")
    @ApiImplicitParams({
            @ApiImplicitParam(value = "頁碼",name = "page", dataType = "int"),
            @ApiImplicitParam(value = "每頁條數", name = "pageSize",dataType ="int")
    })
    //當@PreAuthorize()註解判斷參數為hasRole時，是針對腳色(Role)的判斷方式
    //他效果是會自動給定腳色名稱前面加"ROLE"
    //最後效果@PreAuthorize("hasRole('user'))等價於@PreAuthorize("hasAuthority ('ROLE_user')")
    @PreAuthorize("hasRole('user')")
    public JsonResult<JsonPage<CartStandardVO>> listCartsByPage(
            //控制參數中，實際上也可以判斷某個屬性是否為空，並給定默認值
            @RequestParam(required = false,defaultValue = WebConsts.DEFAULT_PAGE)
            Integer page,
            @RequestParam(required = false,defaultValue = WebConsts.DEFAULT_PAGE_SIZE)
                    Integer pageSize
    ){
        JsonPage<CartStandardVO> jsonPage = omsCartService.listCarts(page,pageSize);
        return JsonResult.ok(jsonPage);
    }

    //根據id的數組，刪除購物車中sku商品
    @PostMapping("/delete")
    @ApiOperation("根據id數組刪除購物車中sku商品")
    @ApiImplicitParam(value = "要刪除的id數組",name = "ids",required = true,dataType = "int")
    @PreAuthorize("hasRole('user')")
    public JsonResult removeByIds(Long[] ids){
        omsCartService.removeCart(ids);
        return JsonResult.ok();
    }
    //清空當前用戶購物車中商品
    @PostMapping("/deleteAll")
    @ApiOperation("清空當前用戶購物車中的所有商品")
    @PreAuthorize("hasRole('user')")
    public JsonResult removeCartByUserId(){
        omsCartService.removeAllCarts();
        return JsonResult.ok("清空完成");
    }
    //修改購物車中的商品數量
    @PostMapping("/update/quantity")
    @ApiOperation("修改用戶中的商品數量")
    @PreAuthorize("hasRole('user')")
    public JsonResult updateQuantity(@Validated CartUpdateDTO cartUpdateDTO){
        omsCartService.updateQuantity(cartUpdateDTO);
        return JsonResult.ok("修改購物車中商品數量成功");
    }



}

package cn.tedu.mall.order.controller;

import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.common.restful.JsonResult;
import cn.tedu.mall.order.service.IOmsOrderService;
import cn.tedu.mall.pojo.order.dto.OrderAddDTO;
import cn.tedu.mall.pojo.order.dto.OrderListTimeDTO;
import cn.tedu.mall.pojo.order.dto.OrderStateUpdateDTO;
import cn.tedu.mall.pojo.order.vo.OrderAddVO;
import cn.tedu.mall.pojo.order.vo.OrderListVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hi 俊翔
 * 現在是下午 08:12 2022/9/26 2022
 * 好好加油吧。
 * 不能再浪費時間了
 * 規劃自己，超越自己
 */
@RestController
@RequestMapping("/oms/order")
@Api(tags = "訂單功能")
public class OmsOrderController {

    @Autowired
    private IOmsOrderService omsOrderService;

    @PostMapping("/add")
    @ApiOperation("新增訂單的方法")
    @PreAuthorize("hasRole('user')")
    public JsonResult<OrderAddVO> addOrder(@Validated OrderAddDTO orderAddDTO){
        OrderAddVO orderAddVO = omsOrderService.addOrder(orderAddDTO);
        return JsonResult.ok(orderAddVO);
    }

    //查詢所有訂單
    @GetMapping("/list")
    @ApiOperation("分頁查詢當前用戶指定時間範圍內的訂單")
    @PreAuthorize("hasRole('user')")
    public JsonResult<JsonPage<OrderListVO>> listUserOrders(OrderListTimeDTO orderListTimeDTO){
        JsonPage<OrderListVO> result = omsOrderService.listOrdersBetweenTimes(orderListTimeDTO);
        return JsonResult.ok(result);
    }

    //修改訂單狀態的方法
    @PostMapping("/update/state")
    @ApiOperation("修改訂單狀態的方法")
    @PreAuthorize("hasRole('user')")
    public JsonResult updateOrderState(OrderStateUpdateDTO orderStateUpdateDTO){
        omsOrderService.updateOrderState(orderStateUpdateDTO);
        return JsonResult.ok("更新成功");
    }

}

package cn.tedu.mall.order.mapper;

import cn.tedu.mall.pojo.order.dto.OrderListTimeDTO;
import cn.tedu.mall.pojo.order.model.OmsOrder;
import cn.tedu.mall.pojo.order.vo.OrderItemListVO;
import cn.tedu.mall.pojo.order.vo.OrderListVO;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Hi 俊翔
 * 現在是下午 03:58 2022/9/24 2022
 * 好好加油吧。
 * 不能再浪費時間了
 * 規劃自己，超越自己
 */
@Repository
public interface OmsOrderMapper {
    //新增訂單對象到數據庫的方法
    void insertOrder(OmsOrder omsOrder);

    //查詢當前用戶指定時間範圍內的所有訂單
    List<OrderListVO> selectOrderBetweenTimes(OrderListTimeDTO orderListTimeDTO);

    //動態修改訂單的sql，根據給定的id值，修改其他各列
    void updateOrderById(OmsOrder omsOrder);
}

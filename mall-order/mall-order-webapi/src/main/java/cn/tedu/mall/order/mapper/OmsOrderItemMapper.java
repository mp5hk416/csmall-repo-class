package cn.tedu.mall.order.mapper;

import cn.tedu.mall.pojo.order.model.OmsOrderItem;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OmsOrderItemMapper {

    //新增訂單的方法
    //一個訂單可以包含多個訂單項目，設計新增時，須考慮到設計成list
    //參數list需要再xml中的sql語句裡用foreach遍例
    void insertOrderItems(List<OmsOrderItem> omsOrderItems);


}

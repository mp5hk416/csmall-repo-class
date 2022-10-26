package cn.tedu.mall.order.mapper;

import cn.tedu.mall.pojo.order.model.OmsCart;
import cn.tedu.mall.pojo.order.vo.CartStandardVO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OmsCartMapper {

    //判斷當前購物車是否已經包含指定的sku商品
    OmsCart selectExistsCart(@Param("userId") Long userId, @Param("skuId") Long skuId);

    //新增sku信息到購物車
    void SaveCart(OmsCart omsCart);

    //修改購物車中指定的sku的數量
    void updateQuantityById(OmsCart omsCart);

    //根據用戶id查詢購物車中的sku信息
    List<CartStandardVO> selectCartsByUserId(Long userId);

    //根據參數數組中的id 刪除購物車中的商品
    int deleteCartsByIds(Long[] ids);

    //清空指定用戶購物車中所有的商品
    int deleteCartByUserId(Long userId);

    //根據用戶id和skuid 刪除商品
    void deleteCartByUserIdAndSkuId(OmsCart omsCart);


}

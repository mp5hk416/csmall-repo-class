package cn.tedu.mall.order.service.impl;

import cn.tedu.mall.common.exception.CoolSharkServiceException;
import cn.tedu.mall.common.pojo.domain.CsmallAuthenticationInfo;
import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.common.restful.ResponseCode;
import cn.tedu.mall.order.mapper.OmsCartMapper;
import cn.tedu.mall.order.service.IOmsCartService;
import cn.tedu.mall.pojo.order.dto.CartAddDTO;
import cn.tedu.mall.pojo.order.dto.CartUpdateDTO;
import cn.tedu.mall.pojo.order.model.OmsCart;
import cn.tedu.mall.pojo.order.vo.CartStandardVO;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import springfox.documentation.spi.service.contexts.SecurityContext;

import java.util.List;

/**
 * Hi 俊翔
 * 現在是下午 09:23 2022/9/23 2022
 * 好好加油吧。
 * 不能再浪費時間了
 * 規劃自己，超越自己
 */
@Service
public class OmsCartServiceImpl implements IOmsCartService {

    @Autowired
    private OmsCartMapper omsCartMapper;

    @Override
    public void addCart(CartAddDTO cartDTO) {
        //獲取當前登陸用戶的id
        Long userId = getUserId();
        //查詢這個userId的用戶是吼已經將
        OmsCart omsCart = omsCartMapper.selectExistsCart(userId, cartDTO.getSkuId());
        if (omsCart!=null) {
            //如果omsCart對象不為空，那麼證明數據表中已經包含這個SKU商品
            //那麼我們要做的事是修改購物車中SKU的數量
            //將omsCart對象中的Quantity相加，然後獻值給omsCart的屬性
            omsCart.setQuantity(omsCart.getQuantity()+cartDTO.getQuantity());
            //調用修改購物車中的sku的數量的方法
            omsCartMapper.updateQuantityById(omsCart);
        }else {
            //如果當前用戶購物車中不存在指定的skuId，就進行新增操作
            //執行新增購物車信息的參數是omsCart
            //現在參數是cartDTO 使用BeanUtils
            OmsCart newOmsCart = new OmsCart();
            BeanUtils.copyProperties(cartDTO,newOmsCart);
            //cartDTO中沒有用戶id信息
            newOmsCart.setUserId(userId);
            //執行新增
            omsCartMapper.SaveCart(newOmsCart);

        }

    }
    //根據用戶id分頁查詢當前購物車sku商品列表
    @Override
    public JsonPage<CartStandardVO> listCarts(Integer page, Integer pageSize) {
        //spring security獲取用戶id
        Long userId = getUserId();
        //pageHelper設置分頁條件
        PageHelper.startPage(page,pageSize);
        //執行查詢，會自動再sql語句後生成limit關鍵字
        List<CartStandardVO> list = omsCartMapper.selectCartsByUserId(userId);
        //將分頁結果對象pageinfo轉成jsonPage進行返回
        return JsonPage.restPage(new PageInfo<>(list));
    }
//刪除指定的id  刪除購物車中的商品
    @Override
    public void removeCart(Long[] ids) {
        //接收mapper執行刪除時返回值，也是刪除成功的行數
        int rows = omsCartMapper.deleteCartsByIds(ids);
        if (rows==0)
            throw new CoolSharkServiceException(ResponseCode.NOT_FOUND,"您要刪除的商品不存在");
    }

    @Override
    public void removeAllCarts() {
        Long userId = getUserId();
        int i = omsCartMapper.deleteCartByUserId(userId);
        if (i==0){
            throw new CoolSharkServiceException(ResponseCode.NOT_FOUND,"沒有找到對應買家購買的商品");
        }
    }
    //生成訂單時，交給dubbo，刪除訂單中選中的商品
    @Override
    public void removeUserCarts(OmsCart omsCart) {
    //omsCart 已經包含了userId和skuId，直接刪除即可
        omsCartMapper.deleteCartByUserIdAndSkuId(omsCart);

    }
    //修改購物車的品數量
    @Override
    public void updateQuantity(CartUpdateDTO cartUpdateDTO) {
        //當前方法參數是cartUpdateDTO
        //這個類型中包含id和數量
        //要想調用修改數量的方法，需要參數類型是omsCart
        //所以需要一個轉化的過程
        OmsCart omsCart = new OmsCart();
        BeanUtils.copyProperties(cartUpdateDTO,omsCart);
        //調用持久層
        omsCartMapper.updateQuantityById(omsCart);
    }

    //業務邏輯層獲取用戶信息的方法，因為多個方法需要獲取用戶信息，這邊獨立寫一個方法
    //這個方法的實現依靠spring security提供登陸用戶的容器
    //方法的目標是獲取springSecurity用戶容器，從容器中獲取用戶信息
    public CsmallAuthenticationInfo getUserInfo(){
        //獲取SpringSecurity
        UsernamePasswordAuthenticationToken authenticationToken = (UsernamePasswordAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
        if (authenticationToken==null)//if 後如果只有執行一句話，則不需要加{}
            throw new CoolSharkServiceException(ResponseCode.UNAUTHORIZED,"沒有登錄信息");
        //如果authenticationToken不為空，則獲得其中用戶信息
        CsmallAuthenticationInfo csmallAuthenticationInfo = (CsmallAuthenticationInfo)authenticationToken.getCredentials();
        return csmallAuthenticationInfo;
    }

    //業務邏輯層大多數方法都是需要用戶id，所以我們編寫一個方法，專門返回當前用戶登陸的id
    public Long getUserId(){
        return getUserInfo().getId();
    }
}

package cn.tedu.mall.order.service.impl;

import cn.tedu.mall.common.exception.CoolSharkServiceException;
import cn.tedu.mall.common.pojo.domain.CsmallAuthenticationInfo;
import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.common.restful.ResponseCode;
import cn.tedu.mall.order.mapper.OmsCartMapper;
import cn.tedu.mall.order.mapper.OmsOrderItemMapper;
import cn.tedu.mall.order.mapper.OmsOrderMapper;
import cn.tedu.mall.order.service.IOmsCartService;
import cn.tedu.mall.order.service.IOmsOrderService;
import cn.tedu.mall.order.utils.IdGeneratorUtils;
import cn.tedu.mall.pojo.order.dto.OrderAddDTO;
import cn.tedu.mall.pojo.order.dto.OrderItemAddDTO;
import cn.tedu.mall.pojo.order.dto.OrderListTimeDTO;
import cn.tedu.mall.pojo.order.dto.OrderStateUpdateDTO;
import cn.tedu.mall.pojo.order.model.OmsCart;
import cn.tedu.mall.pojo.order.model.OmsOrder;
import cn.tedu.mall.pojo.order.model.OmsOrderItem;
import cn.tedu.mall.pojo.order.vo.OrderAddVO;
import cn.tedu.mall.pojo.order.vo.OrderDetailVO;
import cn.tedu.mall.pojo.order.vo.OrderItemListVO;
import cn.tedu.mall.pojo.order.vo.OrderListVO;
import cn.tedu.mall.product.service.order.IForOrderSkuService;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.remoting.exchange.Response;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Hi 俊翔
 * 現在是下午 04:15 2022/9/24 2022
 * 好好加油吧。
 * 不能再浪費時間了
 * 規劃自己，超越自己
 */
@DubboService
@Service
@Slf4j
public class OmsOrderServiceImpl implements IOmsOrderService {

    //利用dubbo
    @DubboReference
    private IForOrderSkuService dubboSkuService;

    @Autowired
    private IOmsCartService omsCartService;

    @Autowired
    private OmsOrderMapper omsOrderMapper;

    @Autowired
    private OmsOrderItemMapper omsOrderItemMapper;


    //因為當前項目使用dubbo來操作其他為服務模塊，他是一個分布式事務操作
    //使用註解激活分布式事務，另seata來保證事務的原子性
    @GlobalTransactional
    @Override
    public OrderAddVO addOrder(OrderAddDTO orderAddDTO) {

        //第一部分收集信息，準備數據
        //操作數據庫的類型是OmsOrder，需要將參數OrderAddDTO中同名屬性復值給OmsOrder
        OmsOrder omsOrder = new OmsOrder();
        BeanUtils.copyProperties(orderAddDTO,omsOrder);

        //因為需要收集和計算業務代碼叫多，單獨編寫一個方法
        loadOrder(omsOrder);
        //上面是為訂單omsOrder進行賦值
        //下面開始為訂單中的訂單項目賦值
        //orderAddDTO中包含訂單中所有的商品，也是訂單項目
        List<OrderItemAddDTO> itemAddDTOS = orderAddDTO.getOrderItems();
        //獲取這些訂單項目，鑒察是否為空
        if (itemAddDTOS==null || itemAddDTOS.isEmpty()){
            //如果訂單向集合為空或者沒有元素，直接拋出異常，終結業務
            throw new CoolSharkServiceException(ResponseCode.BAD_REQUEST,"訂單中必須包含商品");

        }
        //現在我們有List<OrderItemAddDTO集合，集合當中有我們需要的訂單項目
        //但要持久性，需要新增order_item類型List<OmsOrderItem>類型
        //要便利當前的DTOS集合，將其中的對象轉換成OmsOrderItem類型，並保存到新集合中
        List<OmsOrderItem> omsOrderItems = new ArrayList<>();
        //遍例DTOS集合
        for (OrderItemAddDTO itemAddDTO : itemAddDTOS) {
            //首先創建OmsOrderItem對象
            OmsOrderItem omsOrderItem = new OmsOrderItem();
            //利用BeanUtils來轉換
            BeanUtils.copyProperties(itemAddDTO,omsOrderItem);
            //和omsOrder一樣，omsOrderItem也想要單獨一個方法進行判斷賦值
            loadOrderItem(omsOrderItem);
            //上面的方法賦值完成，能夠確認omsOrderItem的id，下面將omsOrder的訂單id賦值給這個對象
            omsOrderItem.setOrderId(omsOrder.getId());
            //omsOrderItem保存到集合中
            omsOrderItems.add(omsOrderItem);
            //到此地一部份完成
            //TODO:第二部分，儲存入庫動作
            //1.減少sku庫存，獲取skuid己少庫存
            Long skuId = omsOrderItem.getSkuId();
            //執行減少庫存的操作(skuid, quantity) 調用dubbo的是product模塊的方法
            int rows = dubboSkuService.reduceStockNum(skuId, omsOrderItem.getQuantity());
            //判斷執行影響行數
            if (rows==0){
                log.warn("商品skuid:{}:商品不足",skuId);
                //減少庫存失敗，需要給用戶提示，拋出異常
                //此時seata會回滾所有之前的操作
                throw new CoolSharkServiceException(ResponseCode.BAD_REQUEST,"庫存不足");
            }
            //刪除購物車信息
            OmsCart omsCart = new OmsCart();
            omsCart.setUserId(omsCart.getUserId());
            omsCart.setSkuId(skuId);
            omsCartService.removeUserCarts(omsCart);
        }

        List<OmsOrderItem> omsOrderItemList = new ArrayList<>();
        for (OrderItemAddDTO itemAddDTO : itemAddDTOS) {
            OmsOrderItem omsOrderItem = new OmsOrderItem();
            BeanUtils.copyProperties(itemAddDTO,omsOrderItem);
            //和OmsOrder一樣，OmsOrderItem也要單獨寫一個方法進行判斷復值
            loadOrderItem(omsOrderItem);

            omsOrderItem.setOrderId(omsOrder.getId());

            omsOrderItemList.add(omsOrderItem);
        }
        //3.新增訂單
        //使用omsOrderMapper的方法完成訂單的新增
        omsOrderMapper.insertOrder(omsOrder);
        //4.新增訂單項目
        //使用omsOrderItemMapper的方法完成訂單項目新增
        omsOrderItemMapper.insertOrderItems(omsOrderItems);
        //最後收集訂單過程中的一些重要數巨，返回給前端
        //程序設計使用OrderAddVO來完成
        OrderAddVO orderAddVO = new OrderAddVO();
        //訂單id
        orderAddVO.setId(omsOrder.getId());
        orderAddVO.setSn(omsOrder.getSn());
        orderAddVO.setCreateTime(omsOrder.getGmtCreate());
        orderAddVO.setPayAmount(omsOrder.getAmountOfActualPay());
        return null;
    }
    //建立loadOrderItem方法
    private void loadOrderItem(OmsOrderItem omsOrderItem){
        if (omsOrderItem.getId()==null){
            Long id = IdGeneratorUtils.getDistributeId("order_item");
            omsOrderItem.setId(id);
        }
        if (omsOrderItem.getSkuId()==null)
            throw new CoolSharkServiceException(ResponseCode.BAD_REQUEST,"訂單中必須要有商品");
    }



    //建立loadOrder方法，方法裡的方法，用私有去修飾
    private void loadOrder(OmsOrder omsOrder){
        //針對omsOrder對象必須具備，為null則進行復值
        if (omsOrder.getId()==null){
            //使用leaf獲得分布式id
            Long id = IdGeneratorUtils.getDistributeId("order");
            omsOrder.setId(id);
        }
        //判斷userId是否為null
        if (omsOrder.getUserId()==null){
            //從spring security上下文中獲取
            omsOrder.setUserId(getUserId());
        }
        //判斷訂單號sn是否為null
        if (omsOrder.getSn()==null){
            //生成訂單號
            omsOrder.setSn(UUID.randomUUID().toString());
        }
        //判斷state並獻值
        if (omsOrder.getState()==null){
            //新生成訂單狀態為未支付=0 支付為1
            omsOrder.setState(0);
        }
        //為了保證生成訂單時間跟創建數據時間以及最後修改時間一致
        //我們本次新增訂單時間使用手動統一復值
        if (omsOrder.getGmtOrder()==null){
            //緒列化後的json文件，seata不認識，這跟底層相關
            //也可以使用Data 生成時間，這樣就可以不用添加kryo依賴以及配置
            LocalDateTime now = LocalDateTime.now();
            omsOrder.setGmtOrder(now);
            omsOrder.setGmtCreate(now);
            omsOrder.setGmtModified(now);
        }
        //後端計算實際支付金額
        //計算公式:原價+運費+優惠=實際支付金額
        //判斷運費和優惠是否為null，如果為null，默認為0
        if (omsOrder.getAmountOfFreight()==null){
            omsOrder.setAmountOfFreight(new BigDecimal(0.0));
        }
        if (omsOrder.getAmountOfDiscount()==null){
            omsOrder.setAmountOfDiscount(new BigDecimal(0.0));
        }
        //判斷原價是否為null，null則報異常
        if (omsOrder.getAmountOfOriginalPrice()==null){
            throw new CoolSharkServiceException(ResponseCode.BAD_REQUEST,"沒有訂單原價");
        }

        //計算實際支付金額
        //原價+運費-優惠
        BigDecimal price = omsOrder.getAmountOfOriginalPrice();
        BigDecimal freight = omsOrder.getAmountOfFreight();
        BigDecimal discount = omsOrder.getAmountOfDiscount();
        BigDecimal actualPrice;
        actualPrice = price.add(freight).subtract(discount);
        //給實際金額進行復值
        omsOrder.setAmountOfActualPay(actualPrice);

    }


    //根據訂單的id修改訂單的狀態業務邏輯層方法
    @Override
    public void updateOrderState(OrderStateUpdateDTO orderStateUpdateDTO) {
    //參數OrderStateUpdateDTO已經包含訂單id和狀態碼了
    //我們修改訂單方法的參數是OmsOrder，所以要實例化該對象並賦值
    OmsOrder omsOrder = new OmsOrder();
    BeanUtils.copyProperties(orderStateUpdateDTO,omsOrder);
    //提交修改狀態訂單的方法
    //動態修改OmsOrder的方法，因為只有ID跟STATE兩個屬性背賦值，其他則不受到影響
    omsOrderMapper.updateOrderById(omsOrder);

    }



    //分頁查詢當前用戶再指定時間範圍(默認一個月內)內所有訂單
    //訂單包含訂單信息焊訂單項目信息(xml語句是多表連查)
    @Override
    public JsonPage<OrderListVO> listOrdersBetweenTimes(OrderListTimeDTO orderListTimeDTO) {
        //因為默認查詢是最近一個月的訂單，所以我們參數startTime和endTime的時候，需要默認值
        //如果有時間，還要檢查起始時間和結束時間是否合理
        validaTimeAndLoadTime(orderListTimeDTO);
        //獲取用戶id
        Long userId = getUserId();
        //orderListTimeDTO肯定沒有userId，將userId賦值給該id
        orderListTimeDTO.setUserId(userId);
        //設治芬頁條鍵
        PageHelper.startPage(orderListTimeDTO.getPage(),orderListTimeDTO.getPageSize());
        //執行查詢
        List<OrderListVO> orderListVOS = omsOrderMapper.selectOrderBetweenTimes(orderListTimeDTO);
        return JsonPage.restPage(new PageInfo<>(orderListVOS));
    }

    //建立方法validaTimeAndLoadTime
    private void validaTimeAndLoadTime(OrderListTimeDTO orderListTimeDto){
        LocalDateTime startTime = orderListTimeDto.getStartTime();
        LocalDateTime endTime = orderListTimeDto.getEndTime();
        //只要起始或結束時間為null，就設置查詢最近一個月訂單
        if (startTime==null||endTime==null){
            //起始時間為現在時間減一個月
            startTime = LocalDateTime.now().minusMonths(1);
            //默認結束時間為當前時間
            endTime = LocalDateTime.now();
            //賦值到orderListTimeDTO中
            orderListTimeDto.setStartTime(startTime);
            orderListTimeDto.setEndTime(endTime);
        }else{
            //如果start跟end都不為null，檢查時間是否符合順序
            //如果時區不同，還要做時區修正
            //如果要支持國際時間的判斷，需要添加時區修正
            if (endTime.toInstant(ZoneOffset.of("+8")).toEpochMilli()<startTime.toInstant(ZoneOffset.of("+8")).toEpochMilli()){
                throw new CoolSharkServiceException(ResponseCode.BAD_REQUEST,"時間設置異常，結束時間不可早於起始時間");
            }
        }
    }




    @Override
    public OrderDetailVO getOrderDetail(Long id) {
        return null;
    }


//建議將以下方法額外建立一個包獨立出去
    public CsmallAuthenticationInfo getUserInfo(){
        //獲取springcontext上下文對象
        UsernamePasswordAuthenticationToken authenticationToken = (UsernamePasswordAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
        if (authenticationToken==null)//if 後如果只有執行一句話，則不需要加{}
            throw new CoolSharkServiceException(ResponseCode.UNAUTHORIZED,"沒有登錄信息");
        //如果authenticationToken不為空，則獲得其中用戶信息
        CsmallAuthenticationInfo csmallAuthenticationInfo = (CsmallAuthenticationInfo)authenticationToken.getCredentials();
        //返回登陸用戶信息
        return csmallAuthenticationInfo;
    }
    public Long getUserId(){
        return getUserInfo().getId();
    }

}

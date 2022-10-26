package cn.tedu.mall.seckill.sevice;

import cn.tedu.mall.common.exception.CoolSharkServiceException;
import cn.tedu.mall.common.pojo.domain.CsmallAuthenticationInfo;
import cn.tedu.mall.common.restful.ResponseCode;
import cn.tedu.mall.order.service.IOmsOrderService;
import cn.tedu.mall.pojo.order.dto.OrderAddDTO;
import cn.tedu.mall.pojo.order.dto.OrderItemAddDTO;
import cn.tedu.mall.pojo.order.vo.OrderAddVO;
import cn.tedu.mall.pojo.seckill.dto.SeckillOrderAddDTO;
import cn.tedu.mall.pojo.seckill.model.Success;
import cn.tedu.mall.pojo.seckill.vo.SeckillCommitVO;
import cn.tedu.mall.seckill.config.RabbitMqComponentConfiguration;
import cn.tedu.mall.seckill.service.ISeckillService;
import cn.tedu.mall.seckill.utils.SeckillCacheUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 時間:2022/10/21  下午 09:29
 * 俊翔，好好加油，找到好工作
 * 項目名:csmall-repo-class
 */

@Service
@Slf4j
public class SeckillServiceImpl implements ISeckillService {

    //需要普通訂單生成的方法
    @DubboReference
    private IOmsOrderService dubboOrderService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    //秒殺訂單提交成功後，消息對列紀錄秒殺成功信息
    @Autowired
    private RabbitTemplate rabbitTemplate;


    //1.判斷當前是否重複購買
    //2.從redis中判斷是否有褲存
    //3.秒殺轉成普通訂單，利用dubbo完成
    //4.利用rabbitmq的方式記錄秒殺成功信息

    @Override
    public SeckillCommitVO commitSeckill(SeckillOrderAddDTO seckillOrderAddDTO) {

        Long skuId = seckillOrderAddDTO.getSeckillOrderItemAddDTO().getSkuId();
        Long userId = getUserId();

        //通過組合userId和SKUiD來確定誰買了什麼商品
        //秒殺業務規定，一個用戶只能購買一個SKUiD依次
        //我們利用用戶ID和SKUID生成一個KEY
        //將KEY保存在REDISˋ中，表示用戶已經購買
        String reseckillCheckKey = SeckillCacheUtils.getReseckillCheckKey(skuId,userId);
        //向redis中保存這個key的信息，可以利用increment()這個方法
        //她效果如下
        //1.如果當前key不存在，redis會創建這個key，並保存她的直為1
        //2.如果當前key存在，redis會給當前key直家1，比如現在是1，運行後會變成2
        //3.最後會將當前key直返回給調用者
        Long seckillTimes = stringRedisTemplate.boundValueOps(reseckillCheckKey).increment();
        if (seckillTimes>1){
            //表示購買次數已經超過1，不是第一次購買，終止業務，拋出異常

            throw new CoolSharkServiceException(ResponseCode.FORBIDDEN,"已經重複消費");

        }
        //沒有大於，表示第一次購買
        //判斷是否有庫存
        //先獲取商品的key
        String seckillSkuCountKey = SeckillCacheUtils.getStockKey(skuId);
        //從redis中獲取庫存數
        //使用descrement();減少，將當前庫存減1
        //leftStock表示剩餘庫存數，0就賣完了。
        Long leftStock = stringRedisTemplate.boundValueOps(seckillSkuCountKey).decrement();
        if (leftStock<0){
            //先刪除購買紀錄
            stringRedisTemplate.boundValueOps(reseckillCheckKey).decrement();
            throw new CoolSharkServiceException(ResponseCode.BAD_REQUEST,"商品沒貨了");
        }
        //程序至此，可以正常購買
        //第二階段，將秒殺訂單轉城普通訂單
        OrderAddDTO orderAddDTO = convertSeckillOrderToOrder(seckillOrderAddDTO);
        //為userId賦值
        orderAddDTO.setUserId(userId);
        //信息完整了，直接使用DUBBO調用成衣班訂單的方法
        OrderAddVO orderAddVO = dubboOrderService.addOrder(orderAddDTO);

        //進入第三階段，使用rabbitmq，進入秒殺成功的信息
        //讓reabbit先題是秒殺成功後，再進行寫入訂單功能，以減緩服務器壓力，主要是滿足異步
        //典型削峰填股，容忍延遲進入數據庫
        //先實例化success對象，將其中的屬性賦值
        Success success = new Success();
        BeanUtils.copyProperties(seckillOrderAddDTO.getSeckillOrderItemAddDTO(),success);
        //將缺少的信息補全
        success.setId(userId);
        success.setOrderSn(orderAddVO.getSn());
        //不直接新增到數據庫理，而是先rabbitmq
        //將數據給RABBITMQ
        rabbitTemplate.convertAndSend(RabbitMqComponentConfiguration.SECKILL_EX
                ,RabbitMqComponentConfiguration.SECKILL_RK);
        //發送到消息對列後，會有消息接收類將SUCCESS信息新增到數據庫
        //當前方法返回值要求是SeckillCommitVO，其中屬性和OrderAddDTO完全一致
        SeckillCommitVO seckillCommitVO = new SeckillCommitVO();
        BeanUtils.copyProperties(orderAddDTO,seckillCommitVO);
        return seckillCommitVO;
    }

    private OrderAddDTO convertSeckillOrderToOrder(SeckillOrderAddDTO seckillOrderAddDTO){
        //實例化OrderAddDTO
        OrderAddDTO orderAddDTO = new OrderAddDTO();
        //復職同名屬性
        BeanUtils.copyProperties(seckillOrderAddDTO,orderAddDTO);
        //seckillOrderAddDTO對象中包含seckillOrderItemAddDTO訂單向對象
        //而OrderAddDTO包含的是OrderItemAddDTO犯行的list集合
        //我們要做的是將seckillOrderItemAddDTO轉城OrderItemAddDTO，並保存到集合中
        OrderItemAddDTO orderItemAddDTO = new OrderItemAddDTO();
        BeanUtils.copyProperties(seckillOrderAddDTO.getSeckillOrderItemAddDTO(),orderItemAddDTO);
        //實例化一個list集合，將orderItemAddDTO賦值其中
        List<OrderItemAddDTO> orderItemAddDTOs = new ArrayList<>();
        orderItemAddDTOs.add(orderItemAddDTO);
        //最好將集合賦值到orderAddDTO中orderItems屬性中並返回
        orderAddDTO.setOrderItems(orderItemAddDTOs);
        return orderAddDTO;

    }

    private CsmallAuthenticationInfo getUserInfo(){
        UsernamePasswordAuthenticationToken authenticationToken =
                (UsernamePasswordAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
        if (authenticationToken==null)
            throw new CoolSharkServiceException(ResponseCode.UNAUTHORIZED,"沒有登錄信息");
        CsmallAuthenticationInfo csmallAuthenticationInfo = (CsmallAuthenticationInfo)authenticationToken.getCredentials();
        return csmallAuthenticationInfo;
    }

    private Long getUserId(){
            return getUserInfo().getId();
    }
}

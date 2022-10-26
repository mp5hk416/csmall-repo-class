package cn.tedu.mall.seckill.exception;

import cn.tedu.mall.common.restful.JsonResult;
import cn.tedu.mall.common.restful.ResponseCode;
import cn.tedu.mall.pojo.seckill.dto.SeckillOrderAddDTO;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import lombok.extern.slf4j.Slf4j;

/**
 * 時間:2022/10/21  下午 09:11
 * 俊翔，好好加油，找到好工作
 * 項目名:csmall-repo-class
 */

@Slf4j
public class SeckillBlockerHandler {
    //聲明限流的方法，返回直必須和控制器一致
    //參數需要包含控制器的參數，最後再添加一個BlockException異常類型的參數
    //這個方法我們定義為靜態方法，方法調用者不需要實例化對象
    public static JsonResult seckill(String randCode, SeckillOrderAddDTO seckillOrderAddDTO, BlockException e){
        log.error("一個請求被限流了");
        return JsonResult.failed(ResponseCode.INTERNAL_SERVER_ERROR,"服務業繁忙!");
    }
}

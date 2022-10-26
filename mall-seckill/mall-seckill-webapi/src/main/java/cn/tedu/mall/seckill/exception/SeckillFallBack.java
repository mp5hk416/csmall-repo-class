package cn.tedu.mall.seckill.exception;

import cn.tedu.mall.common.restful.JsonResult;
import cn.tedu.mall.common.restful.ResponseCode;
import cn.tedu.mall.pojo.seckill.dto.SeckillOrderAddDTO;
import lombok.extern.slf4j.Slf4j;

/**
 * 時間:2022/10/21  下午 09:19
 * 俊翔，好好加油，找到好工作
 * 項目名:csmall-repo-class
 */

//秒殺降級處理
@Slf4j
public class SeckillFallBack {

    //返回值跟控制器保持一致
    //方法參數也是包含控制層方法參數，可以布寫其他參數，也可以添加Throwable 類型參數
    //Throwable 觸發這次降級的原因
    public static JsonResult seckillFall(String randCode, SeckillOrderAddDTO seckillOrderAddDTO, Throwable e){
        log.error("一個請求降級了");
        return JsonResult.failed(ResponseCode.INTERNAL_SERVER_ERROR,e.getMessage());
    }
}

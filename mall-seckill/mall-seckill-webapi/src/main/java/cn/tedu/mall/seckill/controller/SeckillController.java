package cn.tedu.mall.seckill.controller;

import cn.tedu.mall.common.exception.CoolSharkServiceException;
import cn.tedu.mall.common.restful.JsonResult;
import cn.tedu.mall.common.restful.ResponseCode;
import cn.tedu.mall.pojo.seckill.dto.SeckillOrderAddDTO;
import cn.tedu.mall.pojo.seckill.vo.SeckillCommitVO;
import cn.tedu.mall.seckill.exception.SeckillBlockerHandler;
import cn.tedu.mall.seckill.exception.SeckillFallBack;
import cn.tedu.mall.seckill.service.ISeckillService;
import cn.tedu.mall.seckill.utils.SeckillCacheUtils;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;

/**
 * 時間:2022/10/22  上午 10:36
 * 俊翔，好好加油，找到好工作
 * 項目名:csmall-repo-class
 */

@RestController
@RequestMapping("/seckillww")
@Api(tags = "提交秒殺訂單")
public class SeckillController {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ISeckillService seckillService;


    @PostMapping("/{randCode}")
    @ApiOperation("隨機馬驗證並提交訂單")
    @ApiImplicitParam(value =" 隨機馬", name ="randCode",required = true,dataType = "Integer")
    @PreAuthorize("hasRole('user')")
    //限流跟降級的處理
    @SentinelResource(value = "seckill",blockHandlerClass = SeckillBlockerHandler.class,blockHandler = "seckillBlock",fallbackClass = SeckillFallBack.class,fallback = "seckillFall")
    public JsonResult<SeckillCommitVO> commitSeckill(@PathVariable String randCode, SeckillOrderAddDTO seckillOrderAddDTO){
        Long spuId = seckillOrderAddDTO.getSpuId();
        String randCodeKey = SeckillCacheUtils.getRandCodeKey(spuId);

        if (redisTemplate.hasKey(randCodeKey)){
            String redisRandCode = redisTemplate.boundValueOps(randCodeKey).get() + "";
            if (redisRandCode==null){
                //redis信息丟失
                throw new CoolSharkServiceException(ResponseCode.INTERNAL_SERVER_ERROR,"服務器內步錯誤，情重新啟動");
            }
            //判斷隨機馬金額控制器收到隨機馬是否一致，防止投機購買
            if (!redisRandCode.equals(randCode)){
                throw new CoolSharkServiceException(ResponseCode.NOT_FOUND,"隨機碼有誤，沒有指定商品");

            }
            //執行購買動作
            SeckillCommitVO seckillCommitVO = seckillService.commitSeckill(seckillOrderAddDTO);
            return JsonResult.ok(seckillCommitVO);
        }else {
            //如果redis沒有這個商品key值，直接發送異常，題是沒有該商品
            throw new CoolSharkServiceException(ResponseCode.NOT_FOUND,"redis中沒有相應資料，無法指定商品");
        }

    }




}

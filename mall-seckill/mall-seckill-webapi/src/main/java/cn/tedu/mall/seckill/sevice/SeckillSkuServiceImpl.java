package cn.tedu.mall.seckill.sevice;

import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.pojo.product.vo.SkuStandardVO;
import cn.tedu.mall.pojo.seckill.model.SeckillSku;
import cn.tedu.mall.pojo.seckill.vo.SeckillSkuVO;
import cn.tedu.mall.pojo.seckill.vo.SeckillSpuDetailSimpleVO;
import cn.tedu.mall.pojo.seckill.vo.SeckillSpuVO;
import cn.tedu.mall.product.service.seckill.IForSeckillSkuService;
import cn.tedu.mall.seckill.mapper.SeckillSkuMapper;
import cn.tedu.mall.seckill.service.ISeckillSkuService;
import cn.tedu.mall.seckill.service.ISeckillSpuService;
import cn.tedu.mall.seckill.utils.SeckillCacheUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 時間:2022/10/15  下午 02:47
 * 俊翔，好好加油，找到好工作
 * 項目名:csmall-repo-class
 */

@Service
@Slf4j
public class SeckillSkuServiceImpl implements ISeckillSkuService {
    @Autowired
    private SeckillSkuMapper skuMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @DubboReference
    private IForSeckillSkuService dubboSkuService;


    //根據spuid查詢秒殺的sku列表
    //sku中也包含秒殺信息與一班信息
    @Override
    public List<SeckillSkuVO> listSeckillSkus(Long spuId) {

        //根據spuId查詢秒殺表中的sku列表
        List<SeckillSku> seckillSkus = skuMapper.findSeckillSkusBySpuId(spuId);
        //聲明泛行類型為當前業務方法中指定的集合，已備作為返回值
        ArrayList<SeckillSkuVO> seckillSkuVOS = new ArrayList<>();
        //遍例
        for (SeckillSku seckillSku : seckillSkus) {
            //添加其他表屬性的東西進來
            //循環目標實例化seckillskuvo
            //將一班信息和秒殺信息都在其中後新增到seckillskuvos集合中
            SeckillSkuVO seckillSkuVO = new SeckillSkuVO();
            seckillSkuVO=null;
            //獲得skuId，獲得redis中的key值
            Long skuId = seckillSku.getSkuId();
            String seckillSkuKey = SeckillCacheUtils.getSeckillSkuVOKey(skuId);
            if (redisTemplate.hasKey(seckillSkuKey)){
                seckillSkuVO = (SeckillSkuVO)redisTemplate.boundValueOps(seckillSkuKey).get();
            } else {
                //當redis中沒有保存當前key，要連結數據庫查詢，然後保存到redis
                //利用dubbo實現
                SkuStandardVO skuStandardVo = dubboSkuService.getById(skuId);
                seckillSkuVO = new SeckillSkuVO();
                BeanUtils.copyProperties(skuStandardVo,seckillSkuVO);
                //下面將秒殺信息也手動賦值
                seckillSkuVO.setStock(seckillSku.getSeckillStock());
                seckillSkuVO.setSeckillPrice(seckillSku.getSeckillPrice());
                seckillSkuVO.setSeckillLimit(seckillSku.getSeckillLimit());
                //負值完畢，保存到redis
                redisTemplate.boundValueOps(seckillSkuKey).set(seckillSkuVO,125*60+ RandomUtils.nextInt(100),TimeUnit.SECONDS);

            }
            //保存集合中
            seckillSkuVOS.add(seckillSkuVO);
//            SeckillSkuVO seckillSkuVO = new SeckillSkuVO();
//            BeanUtils.copyProperties(seckillSku,seckillSkuVO);
        }
        return seckillSkuVOS;
    }
}

package cn.tedu.mall.seckill.sevice;

import cn.tedu.mall.common.exception.CoolSharkServiceException;
import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.common.restful.ResponseCode;
import cn.tedu.mall.pojo.product.vo.SpuDetailStandardVO;
import cn.tedu.mall.pojo.product.vo.SpuStandardVO;
import cn.tedu.mall.pojo.seckill.dto.SeckillOrderAddDTO;
import cn.tedu.mall.pojo.seckill.model.SeckillSpu;
import cn.tedu.mall.pojo.seckill.vo.SeckillCommitVO;
import cn.tedu.mall.pojo.seckill.vo.SeckillSpuDetailSimpleVO;
import cn.tedu.mall.pojo.seckill.vo.SeckillSpuVO;
import cn.tedu.mall.product.service.seckill.IForSeckillSpuService;
import cn.tedu.mall.seckill.mapper.SeckillSpuMapper;
import cn.tedu.mall.seckill.service.ISeckillService;
import cn.tedu.mall.seckill.service.ISeckillSpuService;
import cn.tedu.mall.seckill.utils.RedisBloomUtils;
import cn.tedu.mall.seckill.utils.SeckillCacheUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 時間:2022/10/14  下午 08:21
 * 俊翔，好好加油，找到好工作
 * 項目名:csmall-repo-class
 */

@Service
@Slf4j
public class SeckillSpuServiceImpl implements ISeckillSpuService {

    //常量類中，沒有定義detail對應key值，所以我們自己定義一個
    public static final String SECKILL_SPU_DETAIL_VO_PREFIX="mall:seckill:spu:detail:vo";

    @Autowired
    private RedisTemplate redisTemplate;


    //查詢秒殺列表 spu的數據
    @Autowired
    private SeckillSpuMapper seckillSpuMapper;

    //秒殺spu表中，沒有商品詳情介紹，需要根據spuid，查詢spu的詳情，所以需要dubbo支持
    //從mall_pms數據庫去查詢spu的詳情信息
    @DubboReference
    private IForSeckillSpuService dubboSeckillSpuService;

    @Autowired
    private RedisBloomUtils redisBloomUtils;


    @Override
    public JsonPage<SeckillSpuVO> listSeckillSpus(Integer page, Integer pageSize) {
        //分業查詢秒殺表中的spu信息
        //設置分頁信息
        PageHelper.startPage(page,pageSize);
        List<SeckillSpu> seckillSpus = seckillSpuMapper.findSeckillSpus();
        //對象類型轉化，需要將seckillSpus中的所有對象轉化為SeckillSpuVO對象才能返回
        //以滿足持久曾定義
        //所以我們下面要準備便利seckillSpus集合，其中所有商品詳情查詢並賦值到seckillSpuVO
        List<SeckillSpuVO> seckillSpuVOs = new ArrayList<>();
        for (SeckillSpu seckillSpu : seckillSpus) {
            //獲取spuid
            Long spuId = seckillSpu.getSpuId();
            //dubbo查詢商品詳情
            SpuStandardVO spuStandardVO = dubboSeckillSpuService.getSpuById(spuId);
            SeckillSpuVO seckillSpuVO = new SeckillSpuVO();
            //賦值
            BeanUtils.copyProperties(spuStandardVO,seckillSpuVO);
            //下面將秒殺表中的相關屬性進行賦值(秒殺價)

            seckillSpuVO.setSeckillListPrice(seckillSpu.getListPrice());
            //賦值起始時間和結束時間
            seckillSpuVO.setStartTime(seckillSpu.getStartTime());
            seckillSpuVO.setEndTime(seckillSpu.getEndTime());
            //將祭包含商品信息又包含秒商商品信息的seckillSpuVO保存到集合中
            seckillSpuVOs.add(seckillSpuVO);

        }
        //返回分頁結果
        return JsonPage.restPage(new PageInfo<>(seckillSpuVOs));
    }
    //根據spuID查詢秒殺商品詳情訊息
    @Override
    public SeckillSpuVO getSeckillSpu(Long spuId) {
        //這裡先判斷當前SPUID是否在不龍過濾器
        //如果不再直接拋出異常
        //獲取本次布隆過濾器的key
        String bloomFilterKey = SeckillCacheUtils.getBloomFilterKey(LocalDate.now());
        log.info("當前批次布隆過濾器key值為:{}",bloomFilterKey);
        if (!redisBloomUtils.bfexists(bloomFilterKey,spuId+"")){
            //這裡表示布隆過濾器沒有spuId，訪指緩存穿透，拋出異常
            throw new CoolSharkServiceException(ResponseCode.NOT_FOUND,"您訪問商品布存在(測試:布隆過濾器)");
        }

        //聲明返回值類型的對象
        SeckillSpuVO seckillSpuVO = null;
        //獲得這個對象對應的key常量
        String seckillSpuVOKey = SeckillCacheUtils.getSeckillSpuVOKey(spuId);
        //執行判斷這個key是否在redis中
        if (redisTemplate.hasKey(seckillSpuVOKey)){
            //如果REDIS中已經存在，直接REDIS獲取
            seckillSpuVO = (SeckillSpuVO) redisTemplate.boundValueOps(seckillSpuVOKey).get();
        }else{
            //如果redis中不存在數據
            //就要從兩方面查詢獲取返回值需要的屬性:pms_spu，seckill_spu
            SeckillSpu seckillSpu = seckillSpuMapper.findAllSeckillSpuId(spuId);
            //判斷spuid查詢不到信息情況下(布隆過濾器誤判時會進行這操作)
            if (seckillSpu == null){
                throw new CoolSharkServiceException(ResponseCode.NOT_FOUND,"訪問商品布存在");
            }
            //上面茶道秒殺信息
            //下面pms_spu表信息
            SpuStandardVO spuById = dubboSeckillSpuService.getSpuById(spuId);
            seckillSpuVO = new SeckillSpuVO();
            BeanUtils.copyProperties(spuById,seckillSpuVO);
            //將秒殺信息賦值
            seckillSpuVO.setSeckillListPrice(seckillSpu.getListPrice());
            seckillSpuVO.setStartTime(seckillSpu.getStartTime());
            seckillSpuVO.setEndTime(seckillSpu.getEndTime());
            //將賦值好的vo對象保存在redis中
            redisTemplate.boundValueOps(seckillSpuVOKey).set(seckillSpuVO,125*60+ RandomUtils.nextInt(100),TimeUnit.SECONDS);
        }
        //判斷當前時間是否在秒殺時間內
        //為了提供效率便不再連接數據庫
        LocalDateTime nowTime = LocalDateTime.now();
        //看當前時間是偶大於開始時間並且小於結束時間
        //時間對象計算時間差duration
        //當時間差為負值時，返回negative
        //判斷當前時間大於開始時間
        Duration afterTime =  Duration.between(nowTime,seckillSpuVO.getStartTime());
        //判斷結束時間大於當前時間
        Duration beforeTime = Duration.between(seckillSpuVO.getEndTime(),nowTime);
        //如果上面兩個對象都是negative，證明當前時間大於開始時間，且小於結束時間
        //此時便可以訪問，須將隨機碼返回前端
        if (afterTime.isNegative() && beforeTime.isNegative()){
            //從redis中獲取隨機碼，並賦值到seckillSpuVO對象URL中
            String randomCodeKey = SeckillCacheUtils.getRandCodeKey(spuId);
            seckillSpuVO.setUrl("/seckill/"+redisTemplate.boundValueOps(randomCodeKey).get());
        }
        //返回對象包含url及隨機碼，這個隨機碼返回給前端保存
        //前端只有利用這個url發出的購物請求，才會給控制器接收處裡

        return seckillSpuVO;
    }

    //根據spuid查詢detail詳情信錫
    @Override
    public SeckillSpuDetailSimpleVO getSeckillSpuDetail(Long spuId) {
        //常量嘔面添加spuId
        String seckillDetailKey = SECKILL_SPU_DETAIL_VO_PREFIX+spuId;
        //聲明當前方法定的返回值對象
        SeckillSpuDetailSimpleVO seckillSpuDetailSimpleVO = null;
        if (redisTemplate.hasKey(seckillDetailKey)){
            //如果redis已經有這個key，則直接賦值給返回對象即可
            seckillSpuDetailSimpleVO =
                    (SeckillSpuDetailSimpleVO) redisTemplate.boundValueOps(seckillDetailKey).get();
        } else {
            //如果redis中沒有這個key，就需要用到數據庫查這個對象
            SpuDetailStandardVO spuDetailStandardVO = dubboSeckillSpuService.getSpuDetailById(spuId);
            seckillSpuDetailSimpleVO = new SeckillSpuDetailSimpleVO();
            //將這個對象中的同名屬性賦值給他
            BeanUtils.copyProperties(spuDetailStandardVO,seckillSpuDetailSimpleVO);
            //將當前賦值完成後的對象保存到redis中
            redisTemplate.boundValueOps(seckillDetailKey).set(seckillSpuDetailSimpleVO,125*60+ RandomUtils.nextInt(100), TimeUnit.SECONDS);
        }
        //返回對象
        return seckillSpuDetailSimpleVO;
    }
}

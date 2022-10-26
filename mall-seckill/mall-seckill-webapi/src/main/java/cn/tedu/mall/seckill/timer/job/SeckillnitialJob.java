package cn.tedu.mall.seckill.timer.job;

import cn.tedu.mall.pojo.seckill.model.SeckillSku;
import cn.tedu.mall.pojo.seckill.model.SeckillSpu;
import cn.tedu.mall.seckill.mapper.SeckillSkuMapper;
import cn.tedu.mall.seckill.mapper.SeckillSpuMapper;
import cn.tedu.mall.seckill.utils.SeckillCacheUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.math.RandomUtils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 時間:2022/10/14  下午 09:44
 * 俊翔，好好加油，找到好工作
 * 項目名:csmall-repo-class
 */

@Slf4j
public class SeckillnitialJob implements Job {

    @Autowired
    private SeckillSpuMapper spuMapper;

    @Autowired
    private SeckillSkuMapper skuMapper;

    @Autowired
    private RedisTemplate redisTemplate;//redis支持在內存內對數字增減，只有數字

    //保存數據是以自符串形式進行儲存，可以在redis內存修改數字格式的數據，庫存要在redis中修改，避免超量飯賣現象
    //數據會因為高併發原因讀寫數據庫，容易在跟數據庫時產生阻塞，而導致數據無法及時修改，流失數據一致性。
    //所以需要StringRedisTemplate進行對內存的寫入修改操作。
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        //本方法是quartz調度運行的，運行時秒殺還沒有開始，要做的事將預熱信息保存在redis中，
        //我們設計的事秒殺開始前五分鐘進行預熱
        //所以我們創建一個5分鐘之後的時間，檢差秒殺是否開始
        LocalDateTime time = LocalDateTime.now().plusMinutes(5);
        //查詢這個時間的所有秒殺商品
        List<SeckillSpu> seckillSpus = spuMapper.findSeckillSpusByTime(time);
        //遍例所有正在秒殺的商品，並將他們的庫存緩存到redis中
        for (SeckillSpu seckillSpu : seckillSpus) {
            //用stringredisTemplate
            //當前spu飾品的品類，需要緩存的庫存(sku的信息)
            //需要根據spuid查詢sku秒殺表
            List<SeckillSku> seckillSkus = skuMapper.findSeckillSkusBySpuId(seckillSpu.getSpuId());
            for (SeckillSku seckillSku : seckillSkus) {
                log.info("開始將商品庫存預熱到redis中",seckillSku);
                //根據skuid獲取對應庫存中redis中的key
                //SeckillCacheUtils.getStockKey能夠獲取的項目為他設置好當前的長量值，參數會追加到最後
                //最終常量值可能為
                //skuStocket = "mall:seckill:sku:stock:1;
                String skuStockKey = SeckillCacheUtils.getStockKey(seckillSku.getSkuId());
                if (redisTemplate.hasKey(skuStockKey)){
                    //如果key已經存在，證明之前已經緩存過
                    log.info("商品:{}已經在緩存中",seckillSku.getSkuId());
                }else{
                    //如果key不再緩存中，需要將當前sku的庫存利用到緩存中
                    //利用stringRedisTemplate操作保存到redis，方便庫存減少
                    //緩存設置過期時間，影該是秒殺活動時間加五分鐘
                    stringRedisTemplate.boundValueOps(skuStockKey).set(
                            //125分鐘加隨機數時間
                            seckillSku.getSeckillStock()+"",125*60+ RandomUtils.nextInt(100), TimeUnit.SECONDS
                    );
                    log.info("保存完畢，添加到庫存{}",seckillSku.getSkuId());

                }
            }
            //上面給每個spu的sku列表商品賦值了庫存數
            //下面要給當前spu賦值隨機碼
            //隨機碼的作用:
            //要想訪問商品必須通過控制器的url訪問
            //比如:/seckill/spu/{id}，如果{id}的位置使用是真實的id值，那麼秒殺開始前就已經將訪問路徑給爆露出來了，也就會出現投機者利用id提前購買到還沒開始秒殺的商品
            //為了防止投機者利用這種方式頭肌，減少服務器壓力，可以將{id}改成一隨機數
            //如果用戶猜這個數字，猜中機率會下降很多
            //即便猜到，後面還會增加不龍過濾器當第二層防護
            //秒殺前五分鐘，才會隨機生成一組隨機碼，有隨機碼才能訪問商品
            String randCodeKey = SeckillCacheUtils.getRandCodeKey(seckillSpu.getSpuId());

            if (!redisTemplate.hasKey(randCodeKey)){
                //如果隨機碼為空，生成隨機碼
                int randomCode = RandomUtils.nextInt(900000)+100000;
                redisTemplate.boundValueOps(randCodeKey).set(randomCode,125*60+RandomUtils.nextInt(100),TimeUnit.SECONDS);
                log.info("spuid為{}號商品生成隨機碼為:{}",seckillSpu.getSpuId(),randomCode);
            }else {
                //為了方便測試，我們在當前位置將隨機馬輸出到控制台
                String randCode = redisTemplate.boundValueOps(randCodeKey).get() + "";
                log.info("spuId為{}的商品的隨機馬:{}",seckillSpu.getSpuId(),randCode);
            }
        }

    }
}

package cn.tedu.mall.seckill.timer.job;

import cn.tedu.mall.seckill.mapper.SeckillSpuMapper;
import cn.tedu.mall.seckill.utils.RedisBloomUtils;
import cn.tedu.mall.seckill.utils.SeckillCacheUtils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 時間:2022/10/22  下午 03:06
 * 俊翔，好好加油，找到好工作
 * 項目名:csmall-repo-class
 */


public class SeckillBloominitialJob implements Job {
    @Autowired
    private RedisBloomUtils redisBloomUtils;

    @Autowired
    private SeckillSpuMapper seckillSpuMapper;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        //首先確定要保存布隆過濾器的批次key
        //我們設計添加兩個秒殺批次的布隆過濾器
        //避免兩個批次之間瞬間的空檔期
        //也允許讓用戶看到下一個批次的商品
        String bloomTodayKey = SeckillCacheUtils.getBloomFilterKey(LocalDate.now());
        String bloomTomorrowKey = SeckillCacheUtils.getBloomFilterKey(LocalDate.now().plusDays(1));

        //實際開發中藥到數據庫中根據秒殺時間(秒殺批次)查詢對應spuId的集合
        //學習過程中，我們只能將全部商品保存到布隆過濾器中(因為只有一個)
        //所以我們查詢當前秒殺spu表中所有spuId集合
        Long[] spuIds = seckillSpuMapper.findAllSeckillSpuIds();
        //布隆過濾器支持String[]數據類型的參數，將數具保存在redis中
        //所以我們要將Long[]轉換為String[]
        String[] spuIdStrs = new String[spuIds.length];
        //將spuIds數組中的元素轉換為String類型賦值到spuIdStrs數組中
        for (int i=0;i<spuIds.length;i++){
            spuIdStrs[i]=spuIds[i]+"";
        }
        //江賦值完的的spuIdStrs保存到布隆過濾器中
        //實際開發中應該查詢兩個批次，每個布隆過濾器保存對應批次商品spuIds
        redisBloomUtils.bfmadd(bloomTodayKey,spuIdStrs);//第一個批次
        redisBloomUtils.bfmadd(bloomTomorrowKey,spuIdStrs);//第二個批次
        System.out.println("兩個批次布隆過濾器家載完成");
    }
}

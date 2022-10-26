package cn.tedu.mall.seckill.timer.job.config;

import cn.tedu.mall.seckill.timer.job.SeckillBloominitialJob;
import cn.tedu.mall.seckill.timer.job.SeckillnitialJob;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.common.recycler.Recycler;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 時間:2022/10/15  上午 10:21
 * 俊翔，好好加油，找到好工作
 * 項目名:csmall-repo-class
 */

@Configuration
@Slf4j
public class QuartzConfig {
    //定義一個JobDeatil
    @Bean
    public JobDetail initJobDetail(){
        log.info("預熱任務綁定");
        return JobBuilder.newJob(SeckillnitialJob.class)
                .withIdentity("intialkill")
                .storeDurably()//持久化
                .build();
    }
    //定義觸發器
    @Bean
    public Trigger initSeckillTrigger(){
        log.info("燠熱觸發氣運行");
        //定義時間，每分鐘運行一次
        CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule("0 0/1 * * * ?");
        return TriggerBuilder.newTrigger()
                .forJob(initJobDetail())
                .withIdentity("initialTrigger")
                .withSchedule(cronScheduleBuilder)
                .build();
    }

    //布隆過濾器進行註冊
    @Bean
    public JobDetail seckillBloomJobDetail(){
        log.info("加載布隆過濾器");
        return JobBuilder.newJob(SeckillBloominitialJob.class)
                .withIdentity("SeckillBloom")
                .storeDurably()
                .build();
    }
    //布隆過濾器觸發氣
    @Bean
    public Trigger seckillBloomTrigger(){
        log.info("觸發氣運行");
        //實際開發根據秒殺批次定義布隆過濾器運行時機
        //學習過程工為了方便，每分鐘運行一次
        CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule("0 0/1 * * * ?");
        return TriggerBuilder.newTrigger()
                .forJob(seckillBloomJobDetail())
                .withIdentity("seckillBloomTrigger")
                .withSchedule(cronScheduleBuilder)
                .build();
    }
}

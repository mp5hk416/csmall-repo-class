package cn.tedu.mall.search.test;

import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.pojo.product.model.Spu;
import cn.tedu.mall.pojo.search.entity.SpuForElastic;
import cn.tedu.mall.product.service.front.IForFrontSpuService;
import cn.tedu.mall.search.repository.SpuForElasticRepository;
import cn.tedu.mall.search.service.ISearchService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;

/**
 * 時間:2022/10/10  下午 09:02
 * 俊翔，好好加油，找到好工作
 * 項目名:csmall-repo-class
 */

@SpringBootTest
@Slf4j
public class TestSearch {

    @Autowired
    private ISearchService searchService;
    @Autowired
    private SpuForElasticRepository elasticRepository;

    @DubboReference
    private IForFrontSpuService dubboSpuService;

    @Autowired
    private SpuForElasticRepository spuForElasticRepository;


//把數據加載到es中
    @Test
    void loadData(){
        searchService.loadSpuByPage();

        System.out.println("ok");
    }

    @Test
    void showall(){
        Page<SpuForElastic> list = elasticRepository.querySearch222("华为", PageRequest.of(0, 2));
        for (SpuForElastic spuForElastic : list) {
            System.out.println(spuForElastic);
        }
    }

    @Test
    void load2(){

        int i = 1;//循環次數，也是頁碼
        int pages = 0; //總頁數
        do {
            //運用dubbo分頁查詢，查詢當前第i頁的數據
            JsonPage<Spu> spuByPage = dubboSpuService.getSpuByPage(i, 2);
            //需要將上面查詢到的spu實體類轉換成SpuForElastic，才能新增到es當中
            ArrayList<SpuForElastic> esSpus = new ArrayList<>();
            //便利spu集合，將其中的對象轉換成SpuForElastic類型
            //並添加到esSpu集合中
            //Jsonpage對象轉list用 getList()方法 獲取分頁數據後遍例
            for (Spu spu : spuByPage.getList()) {
                SpuForElastic spuForElastic = new SpuForElastic();
                BeanUtils.copyProperties(spu,spuForElastic);
                esSpus.add(spuForElastic);
                System.out.println("查詢spu"+spu);
            }
            //執行SpringDataEs 提供的批量新增方法，值行新增到ES的操作
            spuForElasticRepository.saveAll(esSpus);
            log.info("成功加載到第{}頁",i);
            //為下次循環準備
            pages=spuByPage.getTotalPage();
            i++;


        }while(i<=pages);//查詢第幾頁需小於總頁數
    }

}

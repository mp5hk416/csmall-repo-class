package cn.tedu.mall.search.service.impl;

import cn.tedu.mall.common.restful.JsonPage;
import cn.tedu.mall.pojo.product.model.Spu;
import cn.tedu.mall.pojo.search.entity.SpuForElastic;
import cn.tedu.mall.product.service.front.IForFrontSpuService;
import cn.tedu.mall.search.repository.SpuForElasticRepository;
import cn.tedu.mall.search.service.ISearchService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

/**
 * Hi 俊翔
 * 現在是下午 09:29 2022/9/28 2022
 * 好好加油吧。
 * 不能再浪費時間了
 * 規劃自己，超越自己
 */
@Slf4j
@Service
public class SearchServiceImpl implements ISearchService {

    //dubbo調用 product模塊
    @DubboReference
    private IForFrontSpuService dubboSpuService;

    @Autowired
    private SpuForElasticRepository spuForElasticRepository;

    //利用dubbo 從數據庫中查spu，再將查詢出來的spu加載到es中，且整體是循環的
    @Override
    public void loadSpuByPage() {
        //首先查詢一次，這樣就可以根據查詢出來的分頁信息知道總頁數
        //就可以使用do...while
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
            }
            //執行SpringDataEs 提供的批量新增方法，值行新增到ES的操作
            spuForElasticRepository.saveAll(esSpus);
            log.info("成功加載到第{}頁",i);
            //為下次循環準備
            pages=spuByPage.getTotalPage();
            i++;

        }while(i<=pages);//查詢第幾頁需小於總頁數


    }

    @Override
    public JsonPage<SpuForElastic> search(String keyword, Integer page, Integer pageSize) {

        //SpringData 分頁參數page從0開始，如果是0表示第一頁
        //參數賦值要記得先減1
        Page<SpuForElastic> spus = spuForElasticRepository.querySearch222(keyword, PageRequest.of(page - 1, pageSize));
        //業務需要JsonPage   須將Page 轉承JsonPage<>()
        JsonPage<SpuForElastic> jsonPage = new JsonPage<>();
        jsonPage.setPage(page);
        jsonPage.setPageSize(pageSize);
        //總調數和總頁數
        jsonPage.setTotal(spus.getTotalElements());
        jsonPage.setTotalPage(spus.getTotalPages());
        //將查詢到的數據賦值給jsonPage
        jsonPage.setList(spus.getContent());

        return jsonPage;
    }
}

package cn.tedu.mall.search.repository;

import cn.tedu.mall.pojo.search.entity.SpuForElastic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;


//實體類:SpuForElastic  實體類主鍵id類型:Long
//繼承spring data ' 當前接口實現了具備基本的曾刪改茶功能
@Repository
public interface SpuForElasticRepository extends ElasticsearchRepository<SpuForElastic,Long> {

    //自定義查詢方法關鍵字"title"進行糢糊查詢(ex:"手機"
    //Iterable<SpuForElastic> querySpuForElasticsByTitleMatches(String title);

    //我們業務中需要四個字段查詢，總不能一值and 條件1 and 條件2 and 條件3....
    //用@Query()
    //spring data 支持我們於代碼中查詢語句
    //\n:換行
    //\(須轉譯字符):轉譯
    @Query("{\n" +
            "    \"bool\": {\n" +
            "      \"should\": [\n" +
            "        { \"match\": { \"name\": \"?0\"}},\n" +
            "        { \"match\": { \"title\": \"?0\"}},\n" +
            "        { \"match\": { \"description\": \"?0\"}},\n" +
            "        { \"match\": { \"category_name\": \"?0\"}}\n" +
            "        ]\n" +
            "     }\n" +
            "}")
    //上面指定了查詢語句，這個方法的方法名就隨意定義
    Page<SpuForElastic> querySearch222(String keyword, Pageable pageable);


}

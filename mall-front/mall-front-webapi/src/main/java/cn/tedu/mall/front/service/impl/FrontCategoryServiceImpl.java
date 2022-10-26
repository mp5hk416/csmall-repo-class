package cn.tedu.mall.front.service.impl;

import cn.tedu.mall.common.exception.CoolSharkServiceException;
import cn.tedu.mall.common.restful.ResponseCode;
import cn.tedu.mall.front.service.IFrontCategoryService;
import cn.tedu.mall.pojo.front.entity.FrontCategoryEntity;
import cn.tedu.mall.pojo.front.vo.FrontCategoryTreeVO;
import cn.tedu.mall.pojo.product.vo.CategoryStandardVO;
import cn.tedu.mall.product.service.front.IForFrontCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Hi 俊翔
 * 現在是下午 03:34 2022/9/17 2022
 * 好好加油吧。
 * 不能再浪費時間了
 * 規劃自己，超越自己
 */
@Service
@DubboService
@Slf4j
public class FrontCategoryServiceImpl implements IFrontCategoryService {
    //開發規範標準: 為了降低Redis的key拼寫錯誤的風險，我們都定義長量
    public static final String CATEGORY_TREE_KEY = "category_tree";

    //當前模塊查詢所有分類信息對象要依靠product模塊，所有需要dubbo調用product模塊的查詢數據表中所有分類的方法
    @DubboReference
    private IForFrontCategoryService dubboCategoryService;

    //注入redis操作對象
    @Autowired
    private RedisTemplate  redisTemplate;


    @Override
    public FrontCategoryTreeVO<FrontCategoryEntity> categoryTree() {
        //我們會將查詢到的三級分類數結構保存在redis中，所以我們先檢查redis中是否包含上面定義的key
        if (redisTemplate.hasKey(CATEGORY_TREE_KEY)){
            //如果有redis中已經包含分類樹信息，直接用redis查詢返回即可
            FrontCategoryTreeVO<FrontCategoryEntity> treeVO =
                    (FrontCategoryTreeVO<FrontCategoryEntity>)redisTemplate.boundValueOps(CATEGORY_TREE_KEY).get();//k->map->v 給treevo
            //返回樹信息
            return treeVO;
        }

        //redis中沒有三級分類樹信息，當前請求是第一個運行該方法的請求
        //dubbo調用查詢數據庫中所有分類信息對向
        List<CategoryStandardVO> categoryStandardVOS = dubboCategoryService.getCategoryList();
        //調用將數據庫中查詢出來的所有分類信息轉換成三級分類數的方法initTree()
        FrontCategoryTreeVO<FrontCategoryEntity> treeVO = initTree(categoryStandardVOS);
        log.info("數據庫獲取treeVO:{}",treeVO);
        //将转化完成的treeVO保存到Redis中，以备后续请求直接从Redis中获取
        redisTemplate.boundValueOps(CATEGORY_TREE_KEY).set(treeVO,24, TimeUnit.HOURS);
        //千万别忘记返回！！！
        return treeVO;

    }
    private FrontCategoryTreeVO<FrontCategoryEntity> initTree(
            List<CategoryStandardVO> categoryStandardVOS){
        //第一部分：确定所有分类对象的父分类
        //声明一个Map，使用父分类id作为这个map的key，使用当前分类对象集合当这个map的value
        //将所有相同父分类的对象添加到正确的集合中
        Map<Long,List<FrontCategoryEntity>> map = new HashMap<>();
        log.info("当前分类对象总数为：{}",categoryStandardVOS.size());
        //遍历categoryStandardVOS,进行下一步
        for (CategoryStandardVO categoryStandardVO : categoryStandardVOS) {
            //CategoryStandardVO没有children属性，不能保存子分类
            //所以我们要把它转化为保存子分类的FrontCategoryEntity
            FrontCategoryEntity frontCategoryEntity = new FrontCategoryEntity();
            //利用赋值工具类BeanUtils将同名属性赋值到frontCategoryEntity
            BeanUtils.copyProperties(categoryStandardVO,frontCategoryEntity);
            //因为后面要反复使用当前分类对象的父分类id，所以最好取出来
            Long parentId = frontCategoryEntity.getParentId();
            //根据当前分类对象的父分类id向Map添加元素，但是要先判断是否哦已经存在这个Key
            if(map.containsKey(parentId)){
                //如果有这个key，我们直接将当前分类对象添加到map的value的list中
                map.get(parentId).add(frontCategoryEntity);
            }else {
                //如果当前map没有这个key
                //我们要创建List对象，将分类对象保存在这个List中
                List<FrontCategoryEntity> value = new ArrayList<>();
                value.add(frontCategoryEntity);//list.add
                //使用当前parentId做key，上面实例化好的List对象做value保存到map中
                map.put(parentId,value);
            }
        }
        //第二部分：将子分类对象关联到父分类对象的children属性中
        //第一部分中获得的map已经包含了所有父分类包含的子分类对象
        //下面我们就可以从根分类开始，通过循环遍历每个级别的分类对象，添加到对应级别的children属性中
        //一级分类的父id在数据库中设计为0，所以我们代码直接写死获得父分类id为0的所有子分类
        List<FrontCategoryEntity> firstLevels = map.get(0L);
        //判断firstLevels是否为空
        if(firstLevels==null){
            throw new CoolSharkServiceException(ResponseCode.INTERNAL_SERVER_ERROR,"当前项目没有跟分类");
        }
        //遍历所有一级分类对象
        for (FrontCategoryEntity firstLevel : firstLevels) {
            //获得当前一级分类的id,是二级分类的父id
            Long secondLevelParentId = firstLevel.getId();
            //获取当前分类对象的所有子分类(二级分类的集合)
            List<FrontCategoryEntity> secondLevels = map.get(secondLevelParentId);
            //判断二级分类集合是否为空
            if(secondLevels==null){
                log.warn("当前分类缺少二级分类内容:{}",secondLevelParentId);
                //终止本次循环，继续下一次循环
                continue;
            }
            //遍历二级分类集合
            for (FrontCategoryEntity secondLevel : secondLevels) {
                //获取二级分类对象的id，就是三级分类对象的父id
                Long thirdLevelParentId = secondLevel.getId();
                //获取三级分类对象的集合
                List<FrontCategoryEntity> thirdLevels = map.get(thirdLevelParentId);
                //判断thirdLevels是否为空集合
                if (thirdLevels==null){
                    log.warn("当前分类缺少三级分类内容:{}",thirdLevelParentId);
                    //终止本次循环，继续下一次循环
                    continue;
                }
                //将三级分类集合中的对象添加到二级分类对象的children属性中
                secondLevel.setChildrens(thirdLevels);
            }
            //将二级分类集合中的对象添加到一级分类对象的children属性中
            firstLevel.setChildrens(secondLevels);
        }
        //到此为止，所有分类对象的父子关系都已经构建完成
        //但是本方法的返回值FrontCategoryTreeVO<FrontCategoryEntity>对象还没有创建
        FrontCategoryTreeVO<FrontCategoryEntity> treeVo = new FrontCategoryTreeVO<>();
        //将一级分类集合赋值给该对象的属性
        treeVo.setCategories(firstLevels);
        return treeVo;
    }



}

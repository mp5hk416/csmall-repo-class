package cn.tedu.mall.seckill.mapper;

import cn.tedu.mall.pojo.seckill.model.Success;
import org.springframework.stereotype.Repository;

@Repository
public interface SuccessMapper {
    void saveSuccess(Success success);
}

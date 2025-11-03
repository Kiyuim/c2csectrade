package lut.cn.c2cplatform.mapper;

import lut.cn.c2cplatform.entity.Transaction;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TransactionMapper {
    void insert(Transaction transaction);
}

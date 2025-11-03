package lut.cn.c2cplatform.mapper;

import lut.cn.c2cplatform.entity.BargainHelp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BargainHelpMapper {
    void insert(BargainHelp bargainHelp);
    List<BargainHelp> selectByBargainId(@Param("bargainId") Long bargainId);

    // 检查某个用户是否已经帮助过某个砍价活动
    BargainHelp selectByBargainIdAndHelperId(@Param("bargainId") Long bargainId, @Param("helperId") Long helperId);

    // 统计某个砍价活动的助力人数
    Integer countByBargainId(@Param("bargainId") Long bargainId);
}


package lut.cn.c2cplatform.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserRoleMapper {
    int insertUserRole(@Param("userId") Long userId, @Param("roleId") Integer roleId);
}


package lut.cn.c2cplatform.mapper;

import lut.cn.c2cplatform.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface UserMapper {
    User selectById(@Param("id") Long id);
    User selectByUsername(@Param("username") String username);
    // 加载包含角色的用户
    User selectByUsernameWithRoles(@Param("username") String username);
    // 加载所有用户（包含角色）
    List<User> selectAllWithRoles();
    List<User> selectAll();
    int insert(User user);
    int update(User user);
    int deleteById(@Param("id") Long id);
}

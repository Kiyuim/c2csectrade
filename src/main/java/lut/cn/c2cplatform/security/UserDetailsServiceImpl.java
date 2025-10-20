package lut.cn.c2cplatform.security;

import lut.cn.c2cplatform.entity.User;
import lut.cn.c2cplatform.mapper.UserMapper;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserMapper userMapper;
    public UserDetailsServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }
    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 加载包含角色的用户信息
        User user = userMapper.selectByUsernameWithRoles(username);
        if (user == null) {
            throw new UsernameNotFoundException("User Not Found with username: " + username);
        }
        List<GrantedAuthority> authorities = user.getRoles() == null ? List.of() : user.getRoles().stream()
               .map(role -> new SimpleGrantedAuthority(role.getName()))
               .collect(Collectors.toList());
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPasswordHash(),  // 使用 getPasswordHash 而不是 getPassword
                authorities
        );
    }
}

package lut.cn.c2cplatform.service.impl;

import lut.cn.c2cplatform.entity.Role;
import lut.cn.c2cplatform.entity.User;
import lut.cn.c2cplatform.exception.UserAlreadyExistsException;
import lut.cn.c2cplatform.mapper.RoleMapper;
import lut.cn.c2cplatform.mapper.UserMapper;
import lut.cn.c2cplatform.mapper.UserRoleMapper;
import lut.cn.c2cplatform.payload.RegisterRequest;
import lut.cn.c2cplatform.payload.PasswordResetRequest;
import lut.cn.c2cplatform.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

@Service
public class AuthServiceImpl implements AuthService {
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(UserMapper userMapper, RoleMapper roleMapper, UserRoleMapper userRoleMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public User registerUser(RegisterRequest registerRequest) {
        // 检查用户名是否为保留关键字
        if ("admin".equalsIgnoreCase(registerRequest.getUsername())) {
            throw new UserAlreadyExistsException("Error: Username 'admin' is reserved and cannot be used!");
        }
        // 检查用户名是否存在
        if (userMapper.selectByUsername(registerRequest.getUsername()) != null) {
            throw new UserAlreadyExistsException("Error: Username is already taken!");
        }
        // 检查邮箱是否存在
        if (userMapper.selectAll().stream().anyMatch(u -> registerRequest.getEmail().equals(u.getEmail()))) {
            throw new UserAlreadyExistsException("Error: Email is already in use!");
        }
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setDisplayName(registerRequest.getDisplayName());
        user.setEmail(registerRequest.getEmail());
        user.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword()));  // 使用 setPasswordHash
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());

        // 默认用户角色
        Role userRole = roleMapper.selectByName("ROLE_USER");
        if (userRole == null) {
            throw new RuntimeException("Error: Role is not found.");
        }
        user.setRoles(Set.of(userRole));

        // 插入用户并拿到主键ID
        userMapper.insert(user);
        // 写入用户角色关系
        userRoleMapper.insertUserRole(user.getId(), userRole.getId());

        // 返回包含角色的用户
        return userMapper.selectByUsernameWithRoles(user.getUsername());
    }

    @Override
    public User getUserByUsername(String username) {
        return userMapper.selectByUsernameWithRoles(username);
    }

    @Override
    @Transactional
    public boolean resetPassword(PasswordResetRequest request) {
        // 根据用户名查找用户
        User user = userMapper.selectByUsername(request.getUsername());

        if (user == null) {
            return false; // 用户不存在
        }

        // 验证邮箱是否匹配
        if (!user.getEmail().equals(request.getEmail())) {
            return false; // 邮箱不匹配
        }

        // 更新密码
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(Instant.now());
        userMapper.update(user);

        return true;
    }
}

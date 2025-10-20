package lut.cn.c2cplatform.controller;

import lut.cn.c2cplatform.entity.User;
import lut.cn.c2cplatform.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserMapper userMapper;

    public static class UserSummaryDTO {
        public Long id;
        public String username;
        public UserSummaryDTO(Long id, String username) {
            this.id = id;
            this.username = username;
        }
    }

    /**
     * 获取所有用户列表（用于聊天联系人列表），返回精简 DTO
     */
    @GetMapping
    public ResponseEntity<List<UserSummaryDTO>> getAllUsers(Authentication authentication) {
        List<User> users = userMapper.selectAll();
        List<UserSummaryDTO> dtos = users.stream()
                .map(u -> new UserSummaryDTO(u.getId(), u.getUsername()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * 根据用户名获取用户信息（仍返回完整信息，如需可改为 DTO）
     */
    @GetMapping("/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }
}

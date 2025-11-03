package lut.cn.c2cplatform.controller;

import lut.cn.c2cplatform.entity.User;
import lut.cn.c2cplatform.mapper.UserMapper;
import lut.cn.c2cplatform.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private FileStorageService fileStorageService;

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

    /**
     * 上传头像文件
     */
    @PostMapping("/avatar/upload")
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file, Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userMapper.selectByUsername(username);

            if (user == null) {
                return ResponseEntity.status(404).body("用户不存在");
            }

            // 上传文件到MinIO
            String avatarUrl = fileStorageService.uploadFile(file);

            // 更新用户头像URL
            user.setAvatarUrl(avatarUrl);
            user.setUpdatedAt(java.time.Instant.now());
            userMapper.update(user);

            Map<String, String> response = new HashMap<>();
            response.put("avatarUrl", avatarUrl);
            response.put("message", "头像上传成功");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("上传失败: " + e.getMessage());
        }
    }

    /**
     * 通过URL设置头像
     */
    @PostMapping("/avatar/url")
    public ResponseEntity<?> setAvatarByUrl(@RequestBody Map<String, String> request, Authentication authentication) {
        try {
            String username = authentication.getName();
            String avatarUrl = request.get("avatarUrl");

            if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("头像URL不能为空");
            }

            User user = userMapper.selectByUsername(username);
            if (user == null) {
                return ResponseEntity.status(404).body("用户不存在");
            }

            // 更新用户头像URL
            user.setUpdatedAt(java.time.Instant.now());
            user.setAvatarUrl(avatarUrl);
            userMapper.update(user);

            Map<String, String> response = new HashMap<>();
            response.put("avatarUrl", avatarUrl);
            response.put("message", "头像设置成功");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("设置失败: " + e.getMessage());
        }
    }

    /**
     * 重置为默认头像
     */
    @PostMapping("/avatar/reset")
    public ResponseEntity<?> resetAvatar(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userMapper.selectByUsername(username);

            if (user == null) {
                return ResponseEntity.status(404).body("用户不存在");
            }

            user.setUpdatedAt(java.time.Instant.now());
            // 清除用户头像URL
            user.setAvatarUrl(null);
            userMapper.update(user);

            Map<String, String> response = new HashMap<>();
            response.put("message", "头像已重置为默认");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("重置失败: " + e.getMessage());
        }
    }

    /**
     * 修改用户显示名称
     */
    @PutMapping("/display-name")
    public ResponseEntity<?> updateDisplayName(@RequestBody Map<String, String> request, Authentication authentication) {
        try {
            String username = authentication.getName();
            String newDisplayName = request.get("displayName");

            if (newDisplayName == null || newDisplayName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "显示名称不能为空"));
            }

            newDisplayName = newDisplayName.trim();

            // 验证长度
            if (newDisplayName.length() < 2 || newDisplayName.length() > 20) {
                return ResponseEntity.badRequest().body(Map.of("message", "用户名长度必须在2-20个字符之间"));
            }

            // 敏感词检查
            if (containsSensitiveWords(newDisplayName)) {
                return ResponseEntity.badRequest().body(Map.of("message", "用户名包含敏感词汇，请重新输入"));
            }

            // 检查是否与其他用户重复
            if (isDisplayNameExists(newDisplayName, username)) {
                return ResponseEntity.badRequest().body(Map.of("message", "该用户名已被使用，请选择其他名称"));
            }

            User user = userMapper.selectByUsername(username);
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("message", "用户不存在"));
            }
            user.setUpdatedAt(java.time.Instant.now());

            // 更新用户显示名称
            user.setDisplayName(newDisplayName);
            userMapper.update(user);

            Map<String, String> response = new HashMap<>();
            response.put("message", "用户名修改成功");
            response.put("displayName", newDisplayName);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "修改失败: " + e.getMessage()));
        }
    }

    /**
     * 检查敏感词
     */
    private boolean containsSensitiveWords(String name) {
        String[] sensitiveWords = {
            "管理员", "admin", "系统", "system", "root", "超级", "客服",
            "官方", "垃圾", "傻逼", "操你", "妈的", "死", "杀", "色情",
            "政治"
        };

        String lowerName = name.toLowerCase();
        for (String word : sensitiveWords) {
            if (lowerName.contains(word.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查显示名称是否已存在（排除当前用户）
     */
    private boolean isDisplayNameExists(String displayName, String currentUsername) {
        List<User> allUsers = userMapper.selectAll();
        return allUsers.stream()
                .filter(user -> !user.getUsername().equals(currentUsername))
                .anyMatch(user -> displayName.equals(user.getDisplayName()));
    }

    /**
     * 获取当前用户信息（包括余额）
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userMapper.selectByUsername(username);

            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("message", "用户不存在"));
            }

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("username", user.getUsername());
            userInfo.put("displayName", user.getDisplayName());
            userInfo.put("email", user.getEmail());
            userInfo.put("avatarUrl", user.getAvatarUrl());
            userInfo.put("balance", user.getBalance() != null ? user.getBalance() : java.math.BigDecimal.ZERO);

            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "获取用户信息失败: " + e.getMessage()));
        }
    }

    /**
     * 检查用户是否已设置支付密码
     */
    @GetMapping("/payment-password/check")
    public ResponseEntity<?> checkPaymentPassword(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userMapper.selectByUsername(username);

            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("message", "用户不存在"));
            }

            boolean hasPaymentPassword = user.getPaymentPasswordHash() != null &&
                                        !user.getPaymentPasswordHash().trim().isEmpty();

            return ResponseEntity.ok(Map.of("hasPaymentPassword", hasPaymentPassword));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "检查失败: " + e.getMessage()));
        }
    }

    /**
     * 设置支付密码
     */
    @PostMapping("/payment-password/set")
    public ResponseEntity<?> setPaymentPassword(
            @RequestBody lut.cn.c2cplatform.dto.SetPaymentPasswordRequest request,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userMapper.selectByUsername(username);

            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("message", "用户不存在"));
            }

            // 验证密码格式
            if (request.getPassword() == null || !request.getPassword().matches("\\d{6}")) {
                return ResponseEntity.badRequest().body(Map.of("message", "支付密码必须是6位数字"));
            }

            // 验证两次密码是否一致
            if (!request.getPassword().equals(request.getConfirmPassword())) {
                return ResponseEntity.badRequest().body(Map.of("message", "两次输入的密码不一致"));
            }

            // 使用BCrypt加密支付密码
            String encodedPassword = org.springframework.security.crypto.bcrypt.BCrypt.hashpw(
                request.getPassword(),
                org.springframework.security.crypto.bcrypt.BCrypt.gensalt()
            );

            user.setPaymentPasswordHash(encodedPassword);
            user.setUpdatedAt(java.time.Instant.now());
            userMapper.update(user);

            return ResponseEntity.ok(Map.of("message", "支付密码设置成功"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "设置失败: " + e.getMessage()));
        }
    }

    /**
     * 修改支付密码
     */
    @PutMapping("/payment-password/update")
    public ResponseEntity<?> updatePaymentPassword(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userMapper.selectByUsername(username);

            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("message", "用户不存在"));
            }

            String oldPassword = request.get("oldPassword");
            String newPassword = request.get("newPassword");
            String confirmPassword = request.get("confirmPassword");

            // 检查是否已设置支付密码
            if (user.getPaymentPasswordHash() == null || user.getPaymentPasswordHash().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "尚未设置支付密码"));
            }

            // 验证旧密码
            if (!org.springframework.security.crypto.bcrypt.BCrypt.checkpw(oldPassword, user.getPaymentPasswordHash())) {
                return ResponseEntity.badRequest().body(Map.of("message", "原支付密码错误"));
            }

            // 验证新密码格式
            if (newPassword == null || !newPassword.matches("\\d{6}")) {
                return ResponseEntity.badRequest().body(Map.of("message", "新密码必须是6位数字"));
            }

            // 验证两次密码是否一致
            if (!newPassword.equals(confirmPassword)) {
                return ResponseEntity.badRequest().body(Map.of("message", "两次输入的新密码不一致"));
            }

            // 加密新密码
            String encodedPassword = org.springframework.security.crypto.bcrypt.BCrypt.hashpw(
                newPassword,
                org.springframework.security.crypto.bcrypt.BCrypt.gensalt()
            );

            user.setPaymentPasswordHash(encodedPassword);
            user.setUpdatedAt(java.time.Instant.now());
            userMapper.update(user);

            return ResponseEntity.ok(Map.of("message", "支付密码修改成功"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "修改失败: " + e.getMessage()));
        }
    }
}

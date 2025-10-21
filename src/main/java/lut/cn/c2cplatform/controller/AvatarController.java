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
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class AvatarController {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private UserMapper userMapper;

    /**
     * 上传头像文件
     */
    @PostMapping("/avatar/upload")
    public ResponseEntity<?> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userMapper.selectByUsername(username);

            if (user == null) {
                return ResponseEntity.badRequest().body("用户不存在");
            }

            // 上传文件到MinIO
            String avatarUrl = fileStorageService.uploadFile(file);

            // 更新用户头像URL
            user.setAvatarUrl(avatarUrl);
            userMapper.update(user);

            Map<String, String> response = new HashMap<>();
            response.put("avatarUrl", avatarUrl);
            response.put("message", "头像上传成功");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("头像上传失败: " + e.getMessage());
        }
    }

    /**
     * 通过URL设置头像
     */
    @PostMapping("/avatar/url")
    public ResponseEntity<?> setAvatarByUrl(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userMapper.selectByUsername(username);

            if (user == null) {
                return ResponseEntity.badRequest().body("用户不存在");
            }

            String avatarUrl = request.get("avatarUrl");
            if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("头像URL不能为空");
            }

            // 更新用户头像URL
            user.setAvatarUrl(avatarUrl);
            userMapper.update(user);

            Map<String, String> response = new HashMap<>();
            response.put("avatarUrl", avatarUrl);
            response.put("message", "头像设置成功");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("头像设置失败: " + e.getMessage());
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
                return ResponseEntity.badRequest().body("用户不存在");
            }

            // 清除头像URL，使用默认头像
            user.setAvatarUrl(null);
            userMapper.update(user);

            Map<String, String> response = new HashMap<>();
            response.put("message", "已重置为默认头像");
            response.put("avatarUrl", "https://ui-avatars.com/api/?name=" + username + "&background=007bff&color=fff&size=100");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("重置头像失败: " + e.getMessage());
        }
    }
}


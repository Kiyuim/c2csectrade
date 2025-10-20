package lut.cn.c2cplatform.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 用于生成 BCrypt 密码哈希的工具类
 * 运行这个类来生成密码哈希值，然后更新到数据库中
 */
public class PasswordHashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // 生成管理员密码哈希
        String adminPassword = "admin123";
        String adminHash = encoder.encode(adminPassword);
        System.out.println("管理员密码 (admin123) 的哈希值:");
        System.out.println(adminHash);
        System.out.println();

        // 生成测试用户密码哈希
        String userPassword = "password1";
        String userHash = encoder.encode(userPassword);
        System.out.println("测试用户密码 (password1) 的哈希值:");
        System.out.println(userHash);
        System.out.println();

        // 验证哈希是否匹配
        System.out.println("验证 admin123: " + encoder.matches(adminPassword, adminHash));
        System.out.println("验证 password1: " + encoder.matches(userPassword, userHash));
    }
}


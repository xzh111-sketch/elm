package com.lin.elm.conf;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码加密工具
 * 用 BCrypt 做哈希：自带随机盐 + 慢哈希，数据库里存的是不可逆的哈希值
 */
public class PasswordUtil {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    /** 注册或改密码时调用：明文 -> 哈希 */
    public static String encode(String rawPassword) {
        return ENCODER.encode(rawPassword);
    }

    /** 登录时校验密码 */
    public static boolean matches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }
        if (isHashed(storedPassword)) {
            return ENCODER.matches(rawPassword, storedPassword);
        }
        // 兼容老数据：库里还是明文时直接比对，
        // 登录成功后由 UserController 自动升级成哈希（渐进式迁移）
        return storedPassword.equals(rawPassword);
    }

    /** BCrypt 哈希以 $2a$ / $2b$ / $2y$ 开头 */
    public static boolean isHashed(String storedPassword) {
        return storedPassword != null && storedPassword.startsWith("$2");
    }
}

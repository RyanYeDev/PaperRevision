package com.paperrevision.infrastructure.utils;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** 密码工具类 */
@Component
public class PasswordUtils {

    private static final SecureRandom RANDOM = new SecureRandom();

    /** 生成随机盐值 */
    public String generateSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /** 使用SHA-256加密密码 */
    public String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes());
            byte[] hashedPassword = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashedPassword);
        } catch (Exception e) {
            throw new RuntimeException("密码加密失败", e);
        }
    }

    /** 验证密码 */
    public boolean verifyPassword(String password, String salt, String hashedPassword) {
        String newHash = hashPassword(password, salt);
        return newHash.equals(hashedPassword);
    }
}

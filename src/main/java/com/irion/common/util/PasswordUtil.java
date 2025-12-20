package com.irion.common.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordUtil {

    private static final String ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH = 16;

    // 비밀번호 암호화
    public static String encode(String password) {
        try {
            // Salt 생성
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt);
            String saltStr = Base64.getEncoder().encodeToString(salt);

            // 해시 생성
            String hash = hash(password, saltStr);

            return saltStr + ":" + hash;
        } catch (Exception e) {
            throw new RuntimeException("Password encoding failed", e);
        }
    }

    // 비밀번호 검증
    public static boolean matches(String rawPassword, String encodedPassword) {
        try {
            String[] parts = encodedPassword.split(":");
            if (parts.length != 2) {
                return false;
            }

            String salt = parts[0];
            String storedHash = parts[1];
            String computedHash = hash(rawPassword, salt);

            return storedHash.equals(computedHash);
        } catch (Exception e) {
            return false;
        }
    }

    private static String hash(String password, String salt) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(ALGORITHM);
        md.update(salt.getBytes());
        byte[] hashedBytes = md.digest(password.getBytes());
        return Base64.getEncoder().encodeToString(hashedBytes);
    }

    // 비밀번호 생성용 메인 메서드
    // 실행하면 암호화된 비밀번호 출력됨
    public static void main(String[] args) {
        String password = "1234";  // 원하는 비밀번호로 변경
        String encoded = encode(password);
        System.out.println("Password: " + password);
        System.out.println("Encoded: " + encoded);
        System.out.println("Verify: " + matches(password, encoded));
    }

}
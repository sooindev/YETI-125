package com.irion.common.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 비밀번호 해시 (PBKDF2-HMAC-SHA256). 포맷: {@code pbkdf2$<반복>$<salt>$<hash>}
 * 옛 형식(salt:hash)은 미지원 — 옛 백업 복원 시 {@link #main} 으로 재발급
 */
public class PasswordUtil {

    private static final Logger logger = LoggerFactory.getLogger(PasswordUtil.class);

    private static final String PREFIX_PBKDF2 = "pbkdf2$";
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";

    /** OWASP 권고치(2023). 로그인 한 번에 100ms 남짓 */
    private static final int ITERATIONS = 210_000;

    private static final int SALT_LENGTH = 16;
    private static final int KEY_LENGTH_BITS = 256;

    /** 없는 계정용 더미. ITERATIONS 를 따르도록 클래스 로드 시 생성 */
    private static final String DUMMY_HASH = encodeRandom();

    public static String encode(String password) {
        try {
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt);

            byte[] hash = pbkdf2(password, salt, ITERATIONS);

            return PREFIX_PBKDF2 + ITERATIONS + "$"
                    + Base64.getEncoder().encodeToString(salt) + "$"
                    + Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Password encoding failed", e);
        }
    }

    /** 상수 시간 비교 — String.equals 는 소요 시간으로 앞자리를 흘린다 */
    public static boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }

        try {
            if (encodedPassword.startsWith(PREFIX_PBKDF2)) {
                String[] parts = encodedPassword.split("\\$");
                if (parts.length != 4) {
                    return false;
                }

                int iterations = Integer.parseInt(parts[1]);
                byte[] salt = Base64.getDecoder().decode(parts[2]);
                byte[] expected = Base64.getDecoder().decode(parts[3]);

                return MessageDigest.isEqual(expected, pbkdf2(rawPassword, salt, iterations));
            }

            // 화면에는 "비밀번호 오류" 로만 보이므로 실제 원인을 로그에
            logger.error("Unsupported password hash format in storage — "
                    + "expected the pbkdf2$ prefix. Re-issue the hash with PasswordUtil.main");

            // 이 경로만 빨리 거절되면 응답 시간으로 드러남
            burnMatchTime(rawPassword);
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /** 없는 계정에도 같은 시간 소모(항상 false). 안 하면 응답 시간으로 계정 존재가 드러남 */
    public static boolean matchesDummy(String rawPassword) {
        return matches(rawPassword, DUMMY_HASH);
    }

    /** 임의 비밀번호로 현재 설정에 맞춰 생성 */
    private static String encodeRandom() {
        byte[] random = new byte[32];
        new SecureRandom().nextBytes(random);
        return encode(Base64.getEncoder().encodeToString(random));
    }

    /** 재해시 필요 여부. 원문을 아는 시점은 로그인 성공뿐이라 호출부가 그 자리에서 저장 */
    public static boolean needsUpgrade(String encodedPassword) {
        if (encodedPassword == null || !encodedPassword.startsWith(PREFIX_PBKDF2)) {
            return true;
        }

        try {
            String[] parts = encodedPassword.split("\\$");
            return parts.length != 4 || Integer.parseInt(parts[1]) < ITERATIONS;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    /** 검증 한 번 분량의 계산 소모. 결과는 버리고 시간만 맞춤 */
    private static void burnMatchTime(String rawPassword) throws Exception {
        pbkdf2(rawPassword, new byte[SALT_LENGTH], ITERATIONS);
    }

    private static byte[] pbkdf2(String password, byte[] salt, int iterations) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH_BITS);
        try {
            return SecretKeyFactory.getInstance(PBKDF2_ALGORITHM).generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    /** 해시 생성: {@code java PasswordUtil "비밀번호"} */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("사용법: java PasswordUtil \"비밀번호\"");
            return;
        }
        String password = args[0];
        String encoded = encode(password);
        System.out.println(encoded);
    }

}

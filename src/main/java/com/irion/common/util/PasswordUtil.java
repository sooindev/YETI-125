package com.irion.common.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 비밀번호 해시.
 *
 * SHA-256 을 한 번만 돌리던 방식은 GPU 대입에 무방비였다. 요즘 장비는
 * 초당 수십억 번 SHA-256 을 돌린다. salt 는 무지개표만 막아줄 뿐,
 * 대상이 한 계정이면 아무 도움이 되지 않는다.
 *
 * PBKDF2WithHmacSHA256 으로 바꾼다. JDK 표준이라 의존성이 늘지 않고,
 * 반복 횟수만큼 공격 비용도 그대로 늘어난다.
 *
 * 저장 포맷 — 알고리즘을 앞에 적어 두어 나중에 또 바꿀 수 있게 한다.
 *
 *   새 형식 : pbkdf2$&lt;반복횟수&gt;$&lt;salt(Base64)&gt;$&lt;hash(Base64)&gt;
 *   옛 형식 : &lt;salt(Base64)&gt;:&lt;hash(Base64)&gt;      (SHA-256 1회)
 *
 * 옛 형식도 그대로 검증된다. DB 를 미리 갈아엎을 필요 없이, 각자 다음
 * 로그인에 성공하는 순간 새 형식으로 다시 저장된다
 * (AdminServiceImpl 의 재해시 참고). 옛 해시가 다 사라지고 나면
 * matchesLegacy 와 needsUpgrade 를 지우면 된다.
 */
public class PasswordUtil {

    private static final String PREFIX_PBKDF2 = "pbkdf2$";
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";

    /** OWASP 권고치(2023, PBKDF2-HMAC-SHA256). 로그인 한 번에 100ms 남짓 */
    private static final int ITERATIONS = 210_000;

    private static final int SALT_LENGTH = 16;
    private static final int KEY_LENGTH_BITS = 256;

    /** 옛 형식 (SHA-256 1회) */
    private static final String LEGACY_ALGORITHM = "SHA-256";

    // 비밀번호 암호화
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

    /**
     * 비밀번호 검증.
     *
     * 비교는 MessageDigest.isEqual 로 한다. String.equals 는 다른 글자가
     * 나오는 순간 멈춰서, 걸린 시간으로 앞자리가 맞았는지 흘릴 수 있다.
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }

        try {
            if (encodedPassword.startsWith(PREFIX_PBKDF2)) {
                // pbkdf2$반복횟수$salt$hash
                String[] parts = encodedPassword.split("\\$");
                if (parts.length != 4) {
                    return false;
                }

                int iterations = Integer.parseInt(parts[1]);
                byte[] salt = Base64.getDecoder().decode(parts[2]);
                byte[] expected = Base64.getDecoder().decode(parts[3]);

                return MessageDigest.isEqual(expected, pbkdf2(rawPassword, salt, iterations));
            }

            return matchesLegacy(rawPassword, encodedPassword);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 저장된 해시가 옛 형식인가 — 다시 해시해서 저장해야 하는가.
     *
     * 반복 횟수를 올렸을 때도 참이 된다. 원문을 아는 시점(로그인 성공)에만
     * 다시 만들 수 있으므로, 호출부는 그 자리에서 새 값을 저장한다.
     */
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

    private static byte[] pbkdf2(String password, byte[] salt, int iterations) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH_BITS);
        try {
            return SecretKeyFactory.getInstance(PBKDF2_ALGORITHM).generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    /**
     * 옛 형식 검증 — salt:hash, SHA-256 1회.
     *
     * 바이트 변환에 일부러 인코딩을 지정하지 않는다. 지금 DB 에 들어 있는
     * 해시가 인코딩 없는 getBytes() 로 만들어진 값이라, 여기서 UTF-8 을
     * 못 박으면 기존 비밀번호가 안 맞을 수 있다. 이 경로는 재해시가 끝나면
     * 통째로 사라질 코드다.
     */
    @SuppressWarnings("DefaultCharset")
    private static boolean matchesLegacy(String rawPassword, String encodedPassword)
            throws NoSuchAlgorithmException {

        String[] parts = encodedPassword.split(":");
        if (parts.length != 2) {
            return false;
        }

        MessageDigest md = MessageDigest.getInstance(LEGACY_ALGORITHM);
        md.update(parts[0].getBytes());
        byte[] computed = md.digest(rawPassword.getBytes());

        return MessageDigest.isEqual(Base64.getDecoder().decode(parts[1]), computed);
    }

    // 비밀번호 해시 생성용 메인 메서드
    // 사용법: java PasswordUtil "원하는비밀번호"
    // 인자로 받은 비밀번호의 해시값을 출력 (DB tb_admin.admin_password 에 사용)
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

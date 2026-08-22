package com.irion.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
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
 *   pbkdf2$&lt;반복횟수&gt;$&lt;salt(Base64)&gt;$&lt;hash(Base64)&gt;
 *
 * 옛 형식(salt:hash, SHA-256 1회)을 검증하던 경로는 2026-08-22 에 지웠다.
 * tb_admin 의 계정이 모두 새 형식으로 옮겨간 것을 확인한 뒤였다. 그 경로가
 * 남아 있는 동안에는, 옛 해시로 저장된 계정만 검증이 1ms 도 안 걸려서
 * 응답 시간으로 계정을 알아낼 수 있었다 (matchesDummy 주석 참고).
 *
 * 되돌릴 수 없는 변경이다. 옛 형식 해시가 담긴 백업을 되살리면 그 계정은
 * 로그인할 수 없다. 그때는 PasswordUtil 의 main 으로 해시를 새로 만들어
 * tb_admin.admin_password 에 넣어야 한다.
 */
public class PasswordUtil {

    private static final Logger logger = LoggerFactory.getLogger(PasswordUtil.class);

    private static final String PREFIX_PBKDF2 = "pbkdf2$";
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";

    /** OWASP 권고치(2023, PBKDF2-HMAC-SHA256). 로그인 한 번에 100ms 남짓 */
    private static final int ITERATIONS = 210_000;

    private static final int SALT_LENGTH = 16;
    private static final int KEY_LENGTH_BITS = 256;

    /**
     * 없는 계정에 맞춰볼 더미 해시.
     *
     * 클래스가 처음 로드될 때 임의의 값으로 한 번 만든다. 해시 문자열을
     * 소스에 박아두면 ITERATIONS 를 올렸을 때 더미만 옛 비용으로 남아
     * 시간 차이가 도로 벌어진다. 지금 설정으로 만들어야 진짜 검증과
     * 같은 시간이 든다.
     */
    private static final String DUMMY_HASH = encodeRandom();

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

            /*
             * 아는 형식이 아니다.
             *
             * 그냥 false 를 돌려주면 화면에는 "비밀번호가 틀렸다" 로만 보여서,
             * 진짜 원인(옛 형식 해시가 DB 에 들어 있다)을 알아낼 방법이 없다.
             * 백업을 되살렸을 때 여기에 걸린다. 로그에 남겨둔다.
             */
            logger.error("Unsupported password hash format in storage — "
                    + "expected the pbkdf2$ prefix. Re-issue the hash with PasswordUtil.main");

            // 형식이 달라도 시간은 똑같이 쓴다 — 이 계정만 빨리 거절되면
            // 응답 시간으로 "여기 옛 해시가 있다" 가 그대로 드러난다
            burnMatchTime(rawPassword);
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 없는 계정에도 검증과 같은 시간을 쓴다. 언제나 false 다.
     *
     * matches 는 한 번에 100ms 남짓 걸린다. 아이디가 없다고 이 계산을
     * 건너뛰면, 있는 아이디는 ~100ms 없는 아이디는 ~1ms 로 응답이 갈린다.
     * 비밀번호를 몰라도 응답 시간만 재면 "이 아이디는 존재한다"를 알아낼
     * 수 있고, 관리자 계정이 하나뿐이라 그것만으로 표적이 확정된다.
     * 시도 제한과 엮이면 관리자를 10분씩 잠가두기도 쉬워진다.
     *
     * 결과에 의미는 없다. 없는 계정은 어차피 로그인시키지 않는다.
     * 더미 비밀번호는 SecureRandom 으로 만든 값이라 맞을 일도 없다.
     */
    public static boolean matchesDummy(String rawPassword) {
        return matches(rawPassword, DUMMY_HASH);
    }

    /** 더미 해시용 — 아무도 모르는 비밀번호로 지금 설정에 맞춰 만든다 */
    private static String encodeRandom() {
        byte[] random = new byte[32];
        new SecureRandom().nextBytes(random);
        return encode(Base64.getEncoder().encodeToString(random));
    }

    /**
     * 저장된 해시를 다시 만들어 저장해야 하는가.
     *
     * 옛 형식이 사라진 지금도 이 메서드는 남긴다. ITERATIONS 를 올리면
     * 기존 해시가 전부 여기에 걸리기 때문이다. 원문을 아는 시점(로그인
     * 성공)에만 다시 만들 수 있으므로, 호출부는 그 자리에서 새 값을 저장한다.
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

    /**
     * 검증 한 번에 드는 만큼의 계산을 그냥 태운다.
     *
     * 결과는 쓰지 않는다. 어느 경로로 실패하든 걸리는 시간을 같게 만드는
     * 것이 목적이다. salt 는 아무 값이어도 비용이 같으므로 0 으로 둔다.
     */
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

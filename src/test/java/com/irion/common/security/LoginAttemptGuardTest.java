package com.irion.common.security;

import org.junit.Test;

import static org.junit.Assert.*;

/** 로그인 시도 제한. 잠금 동작과 실패 기록이 무한히 쌓이지 않는지 */
public class LoginAttemptGuardTest {

    private static final String IP = "203.0.113.10";
    private static final String OTHER_IP = "198.51.100.20";

    @Test
    public void 실패가_한도에_닿기_전에는_잠기지_않는다() {
        LoginAttemptGuard guard = new LoginAttemptGuard();

        for (int i = 0; i < 4; i++) {
            guard.recordFailure("admin", IP);
        }

        assertEquals(0, guard.lockedSecondsRemaining("admin", IP));
    }

    @Test
    public void 다섯_번_틀리면_잠긴다() {
        LoginAttemptGuard guard = new LoginAttemptGuard();

        for (int i = 0; i < 5; i++) {
            guard.recordFailure("admin", IP);
        }

        long remaining = guard.lockedSecondsRemaining("admin", IP);
        assertTrue("남은 잠금 시간이 있어야 한다: " + remaining, remaining > 0);
        assertTrue("10분을 넘지 않아야 한다: " + remaining, remaining <= 600);
    }

    @Test
    public void 성공하면_기록이_지워진다() {
        LoginAttemptGuard guard = new LoginAttemptGuard();

        guard.recordFailure("admin", IP);
        guard.recordFailure("admin", IP);
        assertEquals(1, guard.trackedCount());

        guard.recordSuccess("admin", IP);

        assertEquals(0, guard.trackedCount());
        assertEquals(0, guard.lockedSecondsRemaining("admin", IP));
    }

    @Test
    public void 대소문자와_공백은_같은_계정으로_센다() {
        LoginAttemptGuard guard = new LoginAttemptGuard();

        guard.recordFailure("admin", IP);
        guard.recordFailure("  ADMIN  ", IP);
        guard.recordFailure("Admin", IP);

        assertEquals("같은 계정이므로 항목은 하나다", 1, guard.trackedCount());
    }

    /** 아이디 5만 개를 부어도 항목은 상한에서 멈춘다 */
    @Test
    public void 가짜_아이디를_쏟아부어도_기록이_무한히_쌓이지_않는다() {
        LoginAttemptGuard guard = new LoginAttemptGuard();

        for (int i = 0; i < 50_000; i++) {
            guard.recordFailure("bot-" + i + "@example.com", IP);
        }

        int tracked = guard.trackedCount();
        assertTrue("상한(10,000) 안에 있어야 한다. 실제: " + tracked, tracked <= 10_000);
    }

    /** 자르지 않으면 1MB 문자열이 그대로 키가 된다 */
    @Test
    public void 아주_긴_아이디도_잘라서_보관한다() {
        LoginAttemptGuard guard = new LoginAttemptGuard();

        StringBuilder huge = new StringBuilder();
        for (int i = 0; i < 100_000; i++) {
            huge.append('a');
        }

        guard.recordFailure(huge.toString(), IP);

        // 앞부분이 같으면 잘린 뒤 같은 키 — 항목이 늘지 않는다
        guard.recordFailure(huge.toString() + "다른꼬리표", IP);
        assertEquals(1, guard.trackedCount());
    }

    /** 잠긴 계정을 밀어내면 가짜 아이디로 잠금을 푸는 우회로가 생긴다 */
    @Test
    public void 넘치더라도_잠긴_계정은_풀리지_않는다() {
        LoginAttemptGuard guard = new LoginAttemptGuard();

        for (int i = 0; i < 5; i++) {
            guard.recordFailure("admin", IP);
        }
        assertTrue(guard.lockedSecondsRemaining("admin", IP) > 0);

        for (int i = 0; i < 50_000; i++) {
            guard.recordFailure("bot-" + i, IP);
        }

        assertTrue("가짜 아이디를 부어도 admin 은 잠긴 채여야 한다",
                guard.lockedSecondsRemaining("admin", IP) > 0);
    }

    /** 아이디만 알면 누구나 관리자를 잠그던 구멍 — 틀린 주소만 잠긴다 */
    @Test
    public void 다른_주소에서_틀려도_내_주소는_잠기지_않는다() {
        LoginAttemptGuard guard = new LoginAttemptGuard();

        for (int i = 0; i < 5; i++) {
            guard.recordFailure("admin", OTHER_IP);
        }

        assertTrue(guard.lockedSecondsRemaining("admin", OTHER_IP) > 0);
        assertEquals(0, guard.lockedSecondsRemaining("admin", IP));
    }

    /** 합친 뒤 자르면 긴 아이디가 주소를 밀어내 모든 주소가 한 칸이 된다 */
    @Test
    public void 긴_아이디라도_주소별로_따로_센다() {
        LoginAttemptGuard guard = new LoginAttemptGuard();

        StringBuilder longId = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            longId.append('a');
        }

        guard.recordFailure(longId.toString(), IP);
        guard.recordFailure(longId.toString(), OTHER_IP);

        assertEquals(2, guard.trackedCount());
    }
}

package com.irion.common.util;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 로그인 시도 제한. 잠금 동작과 함께, 실패 기록이 무한히 쌓이지 않는지 본다.
 */
public class LoginAttemptGuardTest {

    @Test
    public void 실패가_한도에_닿기_전에는_잠기지_않는다() {
        LoginAttemptGuard guard = new LoginAttemptGuard();

        for (int i = 0; i < 4; i++) {
            guard.recordFailure("admin");
        }

        assertEquals(0, guard.lockedSecondsRemaining("admin"));
    }

    @Test
    public void 다섯_번_틀리면_잠긴다() {
        LoginAttemptGuard guard = new LoginAttemptGuard();

        for (int i = 0; i < 5; i++) {
            guard.recordFailure("admin");
        }

        long remaining = guard.lockedSecondsRemaining("admin");
        assertTrue("남은 잠금 시간이 있어야 한다: " + remaining, remaining > 0);
        assertTrue("10분을 넘지 않아야 한다: " + remaining, remaining <= 600);
    }

    @Test
    public void 성공하면_기록이_지워진다() {
        LoginAttemptGuard guard = new LoginAttemptGuard();

        guard.recordFailure("admin");
        guard.recordFailure("admin");
        assertEquals(1, guard.trackedCount());

        guard.recordSuccess("admin");

        assertEquals(0, guard.trackedCount());
        assertEquals(0, guard.lockedSecondsRemaining("admin"));
    }

    @Test
    public void 대소문자와_공백은_같은_계정으로_센다() {
        LoginAttemptGuard guard = new LoginAttemptGuard();

        guard.recordFailure("admin");
        guard.recordFailure("  ADMIN  ");
        guard.recordFailure("Admin");

        assertEquals("같은 계정이므로 항목은 하나다", 1, guard.trackedCount());
    }

    /** 서로 다른 아이디 5만 개를 부어도 항목은 상한에서 멈춰야 한다 */
    @Test
    public void 가짜_아이디를_쏟아부어도_기록이_무한히_쌓이지_않는다() {
        LoginAttemptGuard guard = new LoginAttemptGuard();

        for (int i = 0; i < 50_000; i++) {
            guard.recordFailure("bot-" + i + "@example.com");
        }

        int tracked = guard.trackedCount();
        assertTrue("상한(10,000) 안에 있어야 한다. 실제: " + tracked, tracked <= 10_000);
    }

    /** 자르지 않으면 1MB 짜리 문자열 하나가 그대로 키가 된다 */
    @Test
    public void 아주_긴_아이디도_잘라서_보관한다() {
        LoginAttemptGuard guard = new LoginAttemptGuard();

        StringBuilder huge = new StringBuilder();
        for (int i = 0; i < 100_000; i++) {
            huge.append('a');
        }

        guard.recordFailure(huge.toString());

        // 앞부분이 같으면 잘린 뒤 같은 키가 된다 — 항목이 늘지 않는다
        guard.recordFailure(huge.toString() + "다른꼬리표");
        assertEquals(1, guard.trackedCount());
    }

    /** 잠긴 계정을 밀어내면 가짜 아이디로 잠금을 푸는 우회로가 생긴다 */
    @Test
    public void 넘치더라도_잠긴_계정은_풀리지_않는다() {
        LoginAttemptGuard guard = new LoginAttemptGuard();

        for (int i = 0; i < 5; i++) {
            guard.recordFailure("admin");
        }
        assertTrue(guard.lockedSecondsRemaining("admin") > 0);

        for (int i = 0; i < 50_000; i++) {
            guard.recordFailure("bot-" + i);
        }

        assertTrue("가짜 아이디를 부어도 admin 은 잠긴 채여야 한다",
                guard.lockedSecondsRemaining("admin") > 0);
    }
}

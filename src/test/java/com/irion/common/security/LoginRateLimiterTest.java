package com.irion.common.security;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

/**
 * 주소 기준 로그인 빈도 제한. 창이 열리고 닫히는 동작과 함께,
 * 기록이 무한히 쌓이지 않는지 본다.
 */
public class LoginRateLimiterTest {

    /** 창이 닫히는 순간은 기다려서 볼 수 없다 — 시계를 직접 옮긴다 */
    private final AtomicLong clock = new AtomicLong(1_000_000L);

    private LoginRateLimiter limiter(int maxRequests, long windowMillis) {
        return new LoginRateLimiter(maxRequests, windowMillis, clock::get);
    }

    @Test
    public void 한도까지는_통과시킨다() {
        LoginRateLimiter limiter = limiter(3, 60_000L);

        for (int i = 0; i < 3; i++) {
            assertEquals("3번째까지는 열려 있어야 한다 (" + (i + 1) + "번째)",
                    0, limiter.retryAfterSeconds("1.2.3.4"));
        }
    }

    @Test
    public void 한도를_넘으면_막고_남은_초를_알려준다() {
        LoginRateLimiter limiter = limiter(3, 60_000L);

        for (int i = 0; i < 3; i++) {
            limiter.retryAfterSeconds("1.2.3.4");
        }

        long retryAfter = limiter.retryAfterSeconds("1.2.3.4");
        assertTrue("막혀야 한다", retryAfter > 0);
        assertTrue("창 길이(60초)를 넘지 않아야 한다: " + retryAfter, retryAfter <= 60);
    }

    @Test
    public void 창이_지나면_다시_열린다() {
        LoginRateLimiter limiter = limiter(2, 60_000L);

        limiter.retryAfterSeconds("1.2.3.4");
        limiter.retryAfterSeconds("1.2.3.4");
        assertTrue("한도를 넘겨 막힌 상태여야 한다", limiter.retryAfterSeconds("1.2.3.4") > 0);

        clock.addAndGet(60_000L);

        assertEquals("창이 지났으니 다시 통과해야 한다", 0, limiter.retryAfterSeconds("1.2.3.4"));
    }

    /** 막힌 상태에서 계속 두드려도 창이 뒤로 밀리면 안 된다 — 영영 안 풀린다 */
    @Test
    public void 막힌_뒤에_계속_두드려도_창은_밀리지_않는다() {
        LoginRateLimiter limiter = limiter(1, 60_000L);

        limiter.retryAfterSeconds("1.2.3.4");

        for (int i = 0; i < 100; i++) {
            clock.addAndGet(500L);
            assertTrue("아직 창 안이라 막혀야 한다", limiter.retryAfterSeconds("1.2.3.4") > 0);
        }

        clock.addAndGet(10_000L);   // 첫 요청으로부터 60초가 지났다
        assertEquals(0, limiter.retryAfterSeconds("1.2.3.4"));
    }

    @Test
    public void 주소가_다르면_따로_센다() {
        LoginRateLimiter limiter = limiter(2, 60_000L);

        limiter.retryAfterSeconds("1.2.3.4");
        limiter.retryAfterSeconds("1.2.3.4");
        assertTrue(limiter.retryAfterSeconds("1.2.3.4") > 0);

        assertEquals("옆집이 막혔다고 같이 막히면 안 된다",
                0, limiter.retryAfterSeconds("5.6.7.8"));
    }

    /** 서버 시계가 뒤로 가면 elapsed 가 음수가 된다. 그대로 두면 창이 영영 안 끝난다 */
    @Test
    public void 시계가_뒤로_가도_잠기지_않는다() {
        LoginRateLimiter limiter = limiter(1, 60_000L);

        limiter.retryAfterSeconds("1.2.3.4");
        assertTrue(limiter.retryAfterSeconds("1.2.3.4") > 0);

        clock.addAndGet(-3_600_000L);   // 한 시간 뒤로

        assertEquals("창을 새로 열어야 한다", 0, limiter.retryAfterSeconds("1.2.3.4"));
    }

    @Test
    public void 주소를_쏟아부어도_기록이_무한히_쌓이지_않는다() {
        LoginRateLimiter limiter = limiter(20, 60_000L);

        for (int i = 0; i < 50_000; i++) {
            limiter.retryAfterSeconds("10.0." + (i / 256) + "." + (i % 256));
        }

        int tracked = limiter.trackedCount();
        assertTrue("상한(10,000) 안에 있어야 한다. 실제: " + tracked, tracked <= 10_000);
    }

    /** 창이 끝난 항목은 치우고 새 주소를 받아야 한다 — 안 그러면 상한이 곧 정지 버튼이다 */
    @Test
    public void 창이_끝난_기록은_치우고_새_주소를_받는다() {
        LoginRateLimiter limiter = limiter(20, 60_000L);

        for (int i = 0; i < 50_000; i++) {
            limiter.retryAfterSeconds("10.0." + (i / 256) + "." + (i % 256));
        }

        clock.addAndGet(60_000L);   // 앞선 창이 전부 끝났다

        assertEquals("자리를 비우고 통과시켜야 한다", 0, limiter.retryAfterSeconds("203.0.113.9"));
    }

    /** 헤더에서 온 값이라 길이를 믿지 않는다 */
    @Test
    public void 아주_긴_주소도_잘라서_보관한다() {
        LoginRateLimiter limiter = limiter(20, 60_000L);

        StringBuilder huge = new StringBuilder();
        for (int i = 0; i < 100_000; i++) {
            huge.append('a');
        }

        limiter.retryAfterSeconds(huge.toString());
        limiter.retryAfterSeconds(huge.toString() + "다른꼬리표");

        assertEquals("잘린 뒤 같은 키가 된다", 1, limiter.trackedCount());
    }

    @Test
    public void 주소를_못_알아낸_요청도_세어둔다() {
        LoginRateLimiter limiter = limiter(1, 60_000L);

        assertEquals(0, limiter.retryAfterSeconds(null));
        assertTrue("빈 주소끼리는 한 통에 담긴다", limiter.retryAfterSeconds("") > 0);
    }
}

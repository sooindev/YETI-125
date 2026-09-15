package com.irion.common.security;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * 주소당 로그인 요청 빈도 제한.
 *
 * {@link LoginAttemptGuard} 와 세는 대상이 다르다 — 그쪽은 계정 기준(계정 잠금),
 * 이쪽은 주소 기준(자원 보호). 아이디를 매번 바꾸는 공격은 계정 카운터에 안 걸려 둘 다 필요.
 *
 * 창(window) 방식 — 한도까지 통과, 넘으면 창이 끝날 때까지 차단.
 * 토큰 버킷보다 거칠지만 상태가 정수 둘이라 읽기 쉽다.
 *
 * 톰캣 하나 기준 — 서버를 늘리면 공유 저장소로
 */
public class LoginRateLimiter {

    /**
     * 창당 허용 요청 수. nginx(10r/m + burst 5)보다 느슨하게 —
     * 1차 방어는 nginx, 이쪽은 그물이라 먼저 걸리면 원인을 헷갈리게 한다.
     * 사람이 넘길 일은 없다(계정 잠금이 5회에서 먼저 걸림)
     */
    private static final int DEFAULT_MAX_REQUESTS = 20;

    private static final long DEFAULT_WINDOW_MILLIS = 60 * 1000L;

    /** 추적 상한 — 필요량이 아니라 방어선 */
    private static final int MAX_TRACKED = 10_000;

    /** IPv6 최대 45자. 헤더에서 온 값이라 길이를 믿지 않는다 */
    private static final int MAX_KEY_LENGTH = 45;

    private final int maxRequests;
    private final long windowMillis;
    private final LongSupplier clock;

    private final Map<String, Window> windows = new ConcurrentHashMap<String, Window>();

    public LoginRateLimiter() {
        this(DEFAULT_MAX_REQUESTS, DEFAULT_WINDOW_MILLIS, System::currentTimeMillis);
    }

    /** 테스트가 창을 짧게 잡고 시계를 옮긴다 — 창이 닫히는 순간은 기다려 볼 수 없음 */
    LoginRateLimiter(int maxRequests, long windowMillis, LongSupplier clock) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowMillis;
        this.clock = clock;
    }

    private static final class Window {
        long startedAt;
        int count;

        Window(long startedAt) {
            this.startedAt = startedAt;
        }
    }

    /**
     * 요청 하나를 세고 한도 초과 시 남은 초 반환, 여유 있으면 0.
     * 조회와 증가를 나누지 않는다 — 나누면 그 사이에 다른 스레드가 끼어든다
     */
    public long retryAfterSeconds(String ip) {
        String key = key(ip);
        long now = clock.getAsLong();

        Window window = windows.get(key);

        if (window == null) {
            if (windows.size() >= MAX_TRACKED) {
                sweepExpired(now);
            }
            if (windows.size() >= MAX_TRACKED) {
                // 서로 다른 주소 만 곳이 1분 내 로그인 시도 — 정상이 아니다.
                // 여기서 열어주면 상한이 곧 우회로가 된다
                return (windowMillis + 999) / 1000;
            }
            window = windows.computeIfAbsent(key, k -> new Window(now));
        }

        synchronized (window) {
            long elapsed = now - window.startedAt;

            // elapsed 음수 = 시계 역행. 이때도 창을 새로 연다
            if (elapsed >= windowMillis || elapsed < 0) {
                window.startedAt = now;
                window.count = 1;
                return 0;
            }

            if (window.count >= maxRequests) {
                return (windowMillis - elapsed + 999) / 1000;
            }

            window.count++;
            return 0;
        }
    }

    /** 만료 항목 정리. 창이 짧아 한 번 훑으면 대부분 사라진다 */
    private void sweepExpired(long now) {
        Iterator<Map.Entry<String, Window>> it = windows.entrySet().iterator();
        while (it.hasNext()) {
            Window window = it.next().getValue();
            synchronized (window) {
                if (now - window.startedAt >= windowMillis) {
                    it.remove();
                }
            }
        }
    }

    /** 상한 검증 테스트 대상이라 package-private */
    int trackedCount() {
        return windows.size();
    }

    private static String key(String ip) {
        if (ip == null) {
            return "";
        }
        String key = ip.trim();
        return key.length() <= MAX_KEY_LENGTH ? key : key.substring(0, MAX_KEY_LENGTH);
    }
}

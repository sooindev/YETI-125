package com.irion.common.util;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * 주소 한 곳이 정해진 시간 안에 낼 수 있는 로그인 요청 수를 센다.
 *
 * {@link LoginAttemptGuard} 와 세는 대상이 다르다 — 그쪽은 "이 계정이 몇 번 틀렸나"(계정 잠금),
 * 이쪽은 "이 주소가 몇 번 두드렸나"(자원 보호)다. 아이디를 매번 바꿔 보내는 공격은
 * 계정 카운터에 영원히 걸리지 않으므로 둘 다 있어야 한다.
 *
 * 창(window) 방식이다. 창이 열린 뒤 한도까지는 통과시키고, 넘으면 창이 끝날 때까지 막는다.
 * 토큰 버킷보다 거칠지만 상태가 정수 둘이라 읽기 쉽고, 여기서 필요한 정밀도는 이 정도다.
 *
 * 톰캣 하나 기준 — 서버를 늘리면 공유 저장소로 옮겨야 한다.
 */
public class LoginRateLimiter {

    /**
     * 창 하나에서 받아주는 요청 수. 앞단 nginx(10r/m + burst 5)보다 느슨하게 둔다 —
     * 1차 방어는 nginx 고 이쪽은 그물이라, 평소에 먼저 걸려 원인을 헷갈리게 하면 안 된다.
     *
     * 사람이 이 횟수를 넘길 일은 없다. 계정 잠금이 5회에서 먼저 걸린다.
     */
    private static final int DEFAULT_MAX_REQUESTS = 20;

    private static final long DEFAULT_WINDOW_MILLIS = 60 * 1000L;

    /** 추적 상한 — 정상적으로 필요한 양이 아니라 "여기까지만 받는다"는 선 */
    private static final int MAX_TRACKED = 10_000;

    /** IPv6 를 다 적어도 45자를 넘지 않는다. 헤더에서 온 값이라 길이를 믿지 않는다 */
    private static final int MAX_KEY_LENGTH = 45;

    private final int maxRequests;
    private final long windowMillis;
    private final LongSupplier clock;

    private final Map<String, Window> windows = new ConcurrentHashMap<String, Window>();

    public LoginRateLimiter() {
        this(DEFAULT_MAX_REQUESTS, DEFAULT_WINDOW_MILLIS, System::currentTimeMillis);
    }

    /** 테스트가 창을 짧게 잡고 시계를 직접 옮긴다 — 창이 닫히는 순간은 기다려서 볼 수 없다 */
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
     * 요청 하나를 세고, 한도를 넘었으면 창이 끝날 때까지 남은 초를 돌려준다. 여유가 있으면 0.
     *
     * "물어보기"와 "세기"를 나누지 않는다. 나누면 두 호출 사이에 다른 스레드가 끼어들어
     * 한도를 넘긴 뒤에야 세어지는 자리가 생긴다.
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
                // 서로 다른 주소 만 곳이 1분 안에 로그인을 두드리는 중이다. 정상이 아니므로
                // 통과시키지 않는다 — 여기서 열어주면 상한이 곧 우회로가 된다.
                return (windowMillis + 999) / 1000;
            }
            window = windows.computeIfAbsent(key, k -> new Window(now));
        }

        synchronized (window) {
            long elapsed = now - window.startedAt;

            // elapsed 가 음수면 시계가 뒤로 간 것이다. 그때도 창을 새로 연다
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

    /** 창이 끝난 항목을 지운다. 창 길이가 짧아 한 번 훑으면 대부분 사라진다. */
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

    /** 상한이 지켜지는지 테스트에서 확인하므로 package-private */
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

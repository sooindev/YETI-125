package com.irion.common.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 로그인 시도 제한.
 *
 * 지금까지는 제한이 전혀 없어서, 아이디만 알면 초당 수십 번씩 비밀번호를
 * 밀어 넣을 수 있었다. 관리자 계정이 하나뿐이라 대상도 뻔하다.
 *
 * 계정 이름 기준으로 실패를 센다. IP 기준은 프록시 뒤라 신뢰하기 어렵고,
 * 계정 기준이면 여러 IP 로 나눠 들어와도 같은 카운터에 걸린다. 대신
 * 남의 계정을 일부러 잠가버릴 수 있으므로, 영구 잠금이 아니라 짧은
 * 대기 시간이 지나면 저절로 풀리게 한다.
 *
 * 인스턴스는 톰캣 하나 안에서만 산다. 서버를 여러 대로 늘리면 공유 저장소
 * (예: DB 컬럼)로 옮겨야 한다.
 */
public class LoginAttemptGuard {

    /** 이 횟수를 넘기면 잠근다 */
    private static final int MAX_ATTEMPTS = 5;

    /** 잠금이 풀리기까지 */
    private static final long LOCK_MILLIS = 10 * 60 * 1000L;

    /** 마지막 실패 후 이 시간이 지나면 카운터를 잊는다 */
    private static final long RESET_MILLIS = 30 * 60 * 1000L;

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<String, Attempt>();

    private static final class Attempt {
        final AtomicInteger count = new AtomicInteger();
        volatile long lastFailedAt;
        volatile long lockedUntil;
    }

    /** 지금 잠겨 있으면 남은 초, 아니면 0 */
    public long lockedSecondsRemaining(String loginId) {
        Attempt attempt = attempts.get(key(loginId));
        if (attempt == null) {
            return 0;
        }

        long remaining = attempt.lockedUntil - System.currentTimeMillis();
        return remaining > 0 ? (remaining + 999) / 1000 : 0;
    }

    /** 실패를 기록한다. 한도를 넘으면 잠근다. */
    public void recordFailure(String loginId) {
        String key = key(loginId);
        long now = System.currentTimeMillis();

        Attempt attempt = attempts.computeIfAbsent(key, k -> new Attempt());

        // 한참 만에 다시 틀린 것이면 처음부터 센다
        if (attempt.lastFailedAt != 0 && now - attempt.lastFailedAt > RESET_MILLIS) {
            attempt.count.set(0);
        }

        attempt.lastFailedAt = now;
        if (attempt.count.incrementAndGet() >= MAX_ATTEMPTS) {
            attempt.lockedUntil = now + LOCK_MILLIS;
            attempt.count.set(0);
        }
    }

    /** 성공했으니 잊는다 */
    public void recordSuccess(String loginId) {
        attempts.remove(key(loginId));
    }

    private static String key(String loginId) {
        return loginId == null ? "" : loginId.trim().toLowerCase();
    }
}

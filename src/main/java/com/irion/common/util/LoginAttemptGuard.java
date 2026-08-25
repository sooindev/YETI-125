package com.irion.common.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 로그인 시도 제한. IP 는 프록시 뒤라 못 믿으므로 계정 기준으로 세고, 시간이 지나면 풀린다.
 * 아이디는 공격자가 지어내는 값이라 항목 수와 키 길이에 상한을 둔다.
 * 톰캣 하나 기준 — 서버를 늘리면 공유 저장소로 옮겨야 한다.
 */
public class LoginAttemptGuard {

    /** 이 횟수를 넘기면 잠근다 */
    private static final int MAX_ATTEMPTS = 5;

    /** 잠금이 풀리기까지 */
    private static final long LOCK_MILLIS = 10 * 60 * 1000L;

    /** 마지막 실패 후 이 시간이 지나면 카운터를 잊는다 */
    private static final long RESET_MILLIS = 30 * 60 * 1000L;

    /** 추적 상한 — 정상적으로 필요한 양이 아니라 "여기까지만 받는다"는 선 */
    private static final int MAX_TRACKED = 10_000;

    /** admin_login_id 가 VARCHAR(50) 이라 이보다 길면 실제 계정일 수 없다 */
    private static final int MAX_KEY_LENGTH = 64;

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

        Attempt attempt = attempts.get(key);

        if (attempt == null) {
            // 처음 보는 계정이다. 들이기 전에 자리를 만든다.
            if (attempts.size() >= MAX_TRACKED) {
                makeRoom(now);
            }
            if (attempts.size() >= MAX_TRACKED) {
                // 잠금을 밀어내면서까지 받아주면 가짜 아이디로 진짜 잠금을 씻어낼 수 있다
                return;
            }
            attempt = attempts.computeIfAbsent(key, k -> new Attempt());
        }

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

    /** 만료된 것부터 지우고, 넘치면 잠기지 않은 것 중 오래된 순으로 자른다. */
    private void makeRoom(long now) {
        Iterator<Map.Entry<String, Attempt>> it = attempts.entrySet().iterator();
        while (it.hasNext()) {
            Attempt attempt = it.next().getValue();
            if (attempt.lockedUntil <= now
                    && attempt.lastFailedAt != 0
                    && now - attempt.lastFailedAt > RESET_MILLIS) {
                it.remove();
            }
        }

        if (attempts.size() < MAX_TRACKED) {
            return;
        }

        List<Map.Entry<String, Attempt>> evictable = new ArrayList<Map.Entry<String, Attempt>>();
        for (Map.Entry<String, Attempt> entry : attempts.entrySet()) {
            if (entry.getValue().lockedUntil <= now) {
                evictable.add(entry);
            }
        }
        evictable.sort(Comparator.comparingLong(entry -> entry.getValue().lastFailedAt));

        // 한 자리만 비우면 곧바로 다시 부르게 된다
        int target = attempts.size() - (MAX_TRACKED * 3 / 4);
        for (Map.Entry<String, Attempt> entry : evictable) {
            if (target-- <= 0) {
                break;
            }
            attempts.remove(entry.getKey(), entry.getValue());
        }
    }

    /** 상한이 지켜지는지 테스트에서 확인하므로 package-private */
    int trackedCount() {
        return attempts.size();
    }

    private static String key(String loginId) {
        if (loginId == null) {
            return "";
        }
        String key = loginId.trim().toLowerCase();
        return key.length() <= MAX_KEY_LENGTH ? key : key.substring(0, MAX_KEY_LENGTH);
    }
}

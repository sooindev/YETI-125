package com.irion.common.security;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 로그인 시도 제한 — 계정 + 주소 기준. 한도 초과 시 잠금, 시간이 지나면 해제.
 * 아이디는 공격자가 지어내는 값이라 항목 수·키 길이에 상한을 둔다.
 *
 * 계정만으로 세면 아이디를 아는 누구나 5번 틀려 관리자를 10분씩 계속 잠글 수 있다.
 * 주소를 함께 세면 잠기는 것은 틀린 쪽뿐이다. 여러 주소로 나눠 맞히는 공격은
 * PBKDF2 의 느림과 {@link LoginRateLimiter}(주소당 빈도)가 맡는다.
 *
 * 아이디를 매번 바꾸면 이 카운터에 안 걸린다 — 그쪽은 {@link LoginRateLimiter} 가 주소 기준으로.
 * 톰캣 하나 기준 — 서버를 늘리면 공유 저장소로
 */
public class LoginAttemptGuard {

    /** 잠금 기준 횟수 */
    private static final int MAX_ATTEMPTS = 5;

    /** 잠금 유지 시간 */
    private static final long LOCK_MILLIS = 10 * 60 * 1000L;

    /** 이 시간이 지나면 카운터 초기화 */
    private static final long RESET_MILLIS = 30 * 60 * 1000L;

    /** 추적 상한 — 필요량이 아니라 방어선 */
    private static final int MAX_TRACKED = 10_000;

    /** admin_login_id 가 VARCHAR(50) — 더 길면 실제 계정일 수 없음 */
    private static final int MAX_KEY_LENGTH = 64;

    /** IPv6 최대 45자. 헤더에서 온 값이라 길이를 믿지 않는다 */
    private static final int MAX_IP_LENGTH = 45;

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<String, Attempt>();

    private static final class Attempt {
        final AtomicInteger count = new AtomicInteger();
        volatile long lastFailedAt;
        volatile long lockedUntil;
    }

    /** 잠겨 있으면 남은 초, 아니면 0 */
    public long lockedSecondsRemaining(String loginId, String clientIp) {
        Attempt attempt = attempts.get(key(loginId, clientIp));
        if (attempt == null) {
            return 0;
        }

        long remaining = attempt.lockedUntil - System.currentTimeMillis();
        return remaining > 0 ? (remaining + 999) / 1000 : 0;
    }

    /** 실패 기록. 한도 초과 시 잠금 */
    public void recordFailure(String loginId, String clientIp) {
        String key = key(loginId, clientIp);
        long now = System.currentTimeMillis();

        Attempt attempt = attempts.get(key);

        if (attempt == null) {
            // 처음 보는 계정 — 들이기 전에 자리 확보
            if (attempts.size() >= MAX_TRACKED) {
                makeRoom(now);
            }
            if (attempts.size() >= MAX_TRACKED) {
                // 잠금을 밀어내면 가짜 아이디로 진짜 잠금을 씻어낼 수 있음
                return;
            }
            attempt = attempts.computeIfAbsent(key, k -> new Attempt());
        }

        // 오랜만의 실패면 처음부터
        if (attempt.lastFailedAt != 0 && now - attempt.lastFailedAt > RESET_MILLIS) {
            attempt.count.set(0);
        }

        attempt.lastFailedAt = now;
        if (attempt.count.incrementAndGet() >= MAX_ATTEMPTS) {
            attempt.lockedUntil = now + LOCK_MILLIS;
            attempt.count.set(0);
        }
    }

    /** 성공 — 기록 제거 */
    public void recordSuccess(String loginId, String clientIp) {
        attempts.remove(key(loginId, clientIp));
    }

    /** 만료분 우선 삭제, 넘치면 잠기지 않은 것 중 오래된 순 */
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

        // 한 자리만 비우면 곧바로 다시 호출됨
        int target = attempts.size() - (MAX_TRACKED * 3 / 4);
        for (Map.Entry<String, Attempt> entry : evictable) {
            if (target-- <= 0) {
                break;
            }
            attempts.remove(entry.getKey(), entry.getValue());
        }
    }

    /** 상한 검증 테스트 대상이라 package-private */
    int trackedCount() {
        return attempts.size();
    }

    /** 아이디와 주소를 각각 자른 뒤 잇는다 — 합친 뒤 자르면 긴 아이디가 주소를 밀어낸다 */
    private static String key(String loginId, String clientIp) {
        return truncate(loginId == null ? "" : loginId.trim().toLowerCase(), MAX_KEY_LENGTH)
                + "|" + truncate(clientIp == null ? "" : clientIp.trim(), MAX_IP_LENGTH);
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}

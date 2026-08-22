package com.irion.common.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
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
 * 계정 기준으로 세는 데에는 대가가 따른다. 아이디는 공격자가 지어내는
 * 값이라, 매번 다른 이름으로 실패하면 그만큼 항목이 늘어난다.
 * /admin/loginProc 는 인증도 CSRF 도 거치지 않는 자리라 누구나 두드릴 수
 * 있으므로, 항목 수와 키 길이 양쪽에 상한을 두고 오래된 것을 걷어낸다.
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

    /**
     * 동시에 추적할 계정 수 상한.
     *
     * 실제 관리자는 한 자리 수다. 이 숫자는 "정상적으로 필요한 양"이 아니라
     * "여기까지는 받아주고 그 이상은 버린다"는 선이다.
     */
    private static final int MAX_TRACKED = 10_000;

    /**
     * 키로 보관할 최대 길이.
     *
     * tb_admin.admin_login_id 가 VARCHAR(50) 이라 이보다 긴 아이디는
     * 어차피 존재할 수 없다. 잘라 담아도 실제 계정끼리 겹치지 않는다.
     * 자르지 않으면 1MB 짜리 아이디 하나가 그대로 키가 된다.
     */
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
                /*
                 * 자리를 못 만들었다 — 추적 중인 계정이 전부 잠금 상태라는 뜻이다.
                 * 이 실패는 세지 않고 넘어간다. 이미 걸어둔 잠금을 밀어내면서까지
                 * 새 계정을 받아주면, 가짜 아이디를 쏟아부어 진짜 잠금을 씻어낼
                 * 수 있게 된다. 기록을 하나 놓치는 편이 낫다.
                 */
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

    /**
     * 자리 만들기.
     *
     * 먼저 잊어도 되는 것을 지운다. 잠금이 이미 풀렸고 마지막 실패가
     * RESET_MILLIS 보다 오래된 항목은, 남겨둬도 다음 실패에 카운터가
     * 0 부터 다시 시작하므로 지우는 것과 결과가 같다.
     *
     * 그래도 넘치면 잠기지 않은 항목 중 오래된 순으로 잘라낸다.
     * 잠금이 살아 있는 항목은 건드리지 않는다.
     */
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

        // 한 자리만 비우면 곧바로 다시 부르게 된다. 여유분까지 정리한다.
        int target = attempts.size() - (MAX_TRACKED * 3 / 4);
        for (Map.Entry<String, Attempt> entry : evictable) {
            if (target-- <= 0) {
                break;
            }
            attempts.remove(entry.getKey(), entry.getValue());
        }
    }

    /** 추적 중인 계정 수 — 상한이 지켜지는지 테스트에서 확인하므로 package-private */
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

package com.irion.common.util;

import org.junit.Test;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.Assert.*;

public class PasswordUtilTest {

    @Test
    public void 새_해시는_알고리즘_prefix_를_갖는다() {
        String encoded = PasswordUtil.encode("hunter2");

        assertTrue("pbkdf2 로 시작해야 한다: " + encoded, encoded.startsWith("pbkdf2$"));
        assertEquals("pbkdf2$반복횟수$salt$hash 네 조각", 4, encoded.split("\\$").length);
    }

    @Test
    public void 저장_길이가_컬럼_상한_안에_들어간다() {
        // tb_admin.admin_password 는 VARCHAR(200)
        assertTrue(PasswordUtil.encode("hunter2").length() <= 200);
    }

    @Test
    public void 같은_비밀번호라도_해시가_매번_다르다() {
        assertNotEquals(PasswordUtil.encode("hunter2"), PasswordUtil.encode("hunter2"));
    }

    @Test
    public void 맞는_비밀번호를_통과시킨다() {
        assertTrue(PasswordUtil.matches("hunter2", PasswordUtil.encode("hunter2")));
    }

    @Test
    public void 틀린_비밀번호를_막는다() {
        String encoded = PasswordUtil.encode("hunter2");

        assertFalse(PasswordUtil.matches("hunter3", encoded));
        assertFalse(PasswordUtil.matches("", encoded));
        assertFalse(PasswordUtil.matches(null, encoded));
    }

    @Test
    public void 비_ASCII_비밀번호도_다룬다() {
        String password = "비밀번호-🐧-passphrase";
        assertTrue(PasswordUtil.matches(password, PasswordUtil.encode(password)));
    }

    @Test
    public void 망가진_해시에는_예외를_던지지_않는다() {
        assertFalse(PasswordUtil.matches("hunter2", "쓰레기값"));
        assertFalse(PasswordUtil.matches("hunter2", "pbkdf2$없음"));
        assertFalse(PasswordUtil.matches("hunter2", "pbkdf2$abc$def$ghi"));
        assertFalse(PasswordUtil.matches("hunter2", ""));
        assertFalse(PasswordUtil.matches("hunter2", null));
    }

    // ── 옛 형식(SHA-256 1회) 호환 ─────────────────────────────

    @Test
    public void 옛_형식_해시는_더_이상_통하지_않는다() throws Exception {
        // 2026-08-22, tb_admin 이 모두 새 형식으로 옮겨간 것을 확인하고 지운 경로다.
        // 옛 해시만 검증이 1ms 도 안 걸려서, 응답 시간으로 계정을 알아낼 수 있었다.
        String legacy = legacyEncode("hunter2");

        assertFalse("맞는 비밀번호여도 통과시키지 않는다", PasswordUtil.matches("hunter2", legacy));
        assertFalse(PasswordUtil.matches("hunter3", legacy));
    }

    @Test
    public void 새_형식이_아닌_값은_재해시_대상으로_표시된다() throws Exception {
        assertTrue(PasswordUtil.needsUpgrade(legacyEncode("hunter2")));
        assertTrue(PasswordUtil.needsUpgrade(null));
        assertTrue(PasswordUtil.needsUpgrade("쓰레기값"));
    }

    @Test
    public void 새_형식은_재해시_대상이_아니다() {
        assertFalse(PasswordUtil.needsUpgrade(PasswordUtil.encode("hunter2")));
    }

    @Test
    public void 반복횟수가_모자란_해시는_재해시_대상이다() {
        assertTrue(PasswordUtil.needsUpgrade("pbkdf2$1000$c2FsdA==$aGFzaA=="));
    }

    @Test
    public void 더미_검증은_언제나_실패한다() {
        assertFalse(PasswordUtil.matchesDummy("hunter2"));
        assertFalse(PasswordUtil.matchesDummy(""));
        assertFalse(PasswordUtil.matchesDummy(null));
    }

    /**
     * 더미 검증은 진짜 검증과 같은 비용이어야 의미가 있다.
     * 더미만 싸게 끝나면 시간 차이가 도로 벌어진다.
     */
    @Test
    public void 더미_검증도_진짜_검증만큼_시간을_쓴다() {
        String stored = PasswordUtil.encode("hunter2");

        long real = fastest(() -> PasswordUtil.matches("틀린값", stored));
        long dummy = fastest(() -> PasswordUtil.matchesDummy("틀린값"));

        assertTrue(String.format("더미가 너무 빠르다 — 진짜 %.1fms / 더미 %.1fms",
                        real / 1_000_000.0, dummy / 1_000_000.0),
                dummy * 2 >= real);
    }

    /** 여러 번 돌려 가장 빨랐던 시간(ns) — 다른 프로세스의 방해를 덜 받는다 */
    private static long fastest(Runnable task) {
        long best = Long.MAX_VALUE;
        for (int i = 0; i < 5; i++) {
            long started = System.nanoTime();
            task.run();
            best = Math.min(best, System.nanoTime() - started);
        }
        return best;
    }

    /** 옛 구현이 만들던 값 그대로 — salt:hash, SHA-256 1회 */
    private static String legacyEncode(String password) throws Exception {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        String saltStr = Base64.getEncoder().encodeToString(salt);

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(saltStr.getBytes());
        return saltStr + ":" + Base64.getEncoder().encodeToString(md.digest(password.getBytes()));
    }
}

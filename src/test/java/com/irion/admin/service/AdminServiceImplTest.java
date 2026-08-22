package com.irion.admin.service;

import com.irion.admin.mapper.AdminMapper;
import com.irion.admin.service.impl.AdminServiceImpl;
import com.irion.admin.vo.AdminVO;
import com.irion.common.util.PasswordUtil;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * 관리자 로그인 검증.
 *
 * 여기서 보는 것은 "맞으면 통과, 틀리면 거절" 만이 아니다. 없는 아이디로
 * 들어왔을 때 얼마나 빨리 거절하는지도 봐야 한다. 예전에는 아이디가 없으면
 * 해시 계산을 통째로 건너뛰고 곧장 돌아섰다. 그래서 있는 아이디는 ~100ms,
 * 없는 아이디는 ~1ms 로 갈렸고, 비밀번호를 모르는 사람도 응답 시간만 재면
 * 계정이 있는지 알 수 있었다.
 */
public class AdminServiceImplTest {

    /** 로그인은 실패하지만 계정은 존재하는 상황을 만든다 */
    private static final String EXISTING_ID = "admin";
    private static final String WRONG_PASSWORD = "이건틀린비밀번호";

    @Test
    public void 맞는_비밀번호면_통과한다() {
        AdminService service = service("hunter2");

        AdminVO admin = service.login(EXISTING_ID, "hunter2");

        assertNotNull(admin);
        assertEquals(EXISTING_ID, admin.getAdminLoginId());
    }

    @Test
    public void 틀린_비밀번호면_거절한다() {
        AdminService service = service("hunter2");

        assertNull(service.login(EXISTING_ID, WRONG_PASSWORD));
    }

    @Test
    public void 없는_아이디면_거절한다() {
        AdminService service = service("hunter2");

        assertNull(service.login("존재하지않는계정", WRONG_PASSWORD));
    }

    /**
     * 이 테스트가 고치기 전 코드에서는 실패한다.
     *
     * 두 경우 모두 로그인에 실패하지만, 걸리는 시간이 비슷해야 한다.
     * 잡음을 줄이려고 여러 번 재서 가장 빨랐던 값을 쓴다 — 최솟값은
     * 다른 프로세스의 방해를 가장 덜 받는다.
     */
    @Test
    public void 없는_아이디도_있는_아이디와_비슷한_시간이_걸린다() {
        AdminService service = service("hunter2");

        long existing = fastestLoginNanos(service, EXISTING_ID);
        long missing = fastestLoginNanos(service, "존재하지않는계정");

        // 고치기 전에는 여기서 수백~수천 배가 났다
        assertTrue(
                String.format("없는 아이디가 너무 빨리 돌아온다 — 있는 계정 %.1fms / 없는 계정 %.1fms",
                        existing / 1_000_000.0, missing / 1_000_000.0),
                missing * 2 >= existing);
    }

    /** 로그인에 성공하면 마지막 로그인 시각을 갱신한다 */
    @Test
    public void 성공하면_마지막_로그인_시각을_남긴다() {
        List<String> calls = new ArrayList<String>();
        AdminService service = service("hunter2", calls);

        service.login(EXISTING_ID, "hunter2");

        assertTrue("updateLastLoginDate 가 불려야 한다", calls.contains("updateLastLoginDate"));
    }

    @Test
    public void 실패하면_아무것도_갱신하지_않는다() {
        List<String> calls = new ArrayList<String>();
        AdminService service = service("hunter2", calls);

        service.login(EXISTING_ID, WRONG_PASSWORD);

        assertFalse(calls.contains("updateLastLoginDate"));
        assertFalse(calls.contains("updatePassword"));
    }

    /**
     * 옛 형식(SHA-256 1회) 해시로는 로그인할 수 없다.
     *
     * 검증 경로를 2026-08-22 에 지웠다. 옛 해시는 검증이 1ms 도 안 걸려서,
     * 그런 계정만 응답이 빨리 돌아와 존재가 드러났다.
     *
     * 되돌릴 수 없는 변경이다 — 옛 해시가 담긴 백업을 되살리면 그 계정은
     * 로그인할 수 없고, PasswordUtil.main 으로 해시를 새로 넣어야 한다.
     */
    @Test
    public void 옛_해시로는_로그인할_수_없다() throws Exception {
        List<String> calls = new ArrayList<String>();
        AdminService service = serviceWithHash(legacyEncode("hunter2"), calls);

        assertNull("맞는 비밀번호여도 거절한다", service.login(EXISTING_ID, "hunter2"));
        assertFalse(calls.contains("updateLastLoginDate"));
    }

    /** 옛 해시로 거절될 때도 응답 시간은 다른 실패와 같아야 한다 */
    @Test
    public void 옛_해시_계정도_다른_실패와_비슷한_시간이_걸린다() throws Exception {
        AdminService legacy = serviceWithHash(legacyEncode("hunter2"), new ArrayList<String>());
        AdminService current = service("hunter2");

        long legacyNanos = fastestLoginNanos(legacy, EXISTING_ID);
        long currentNanos = fastestLoginNanos(current, EXISTING_ID);

        assertTrue(
                String.format("옛 해시 계정이 너무 빨리 돌아온다 — 새 형식 %.1fms / 옛 형식 %.1fms",
                        currentNanos / 1_000_000.0, legacyNanos / 1_000_000.0),
                legacyNanos * 2 >= currentNanos);
    }

    @Test
    public void 이미_새_형식이면_다시_저장하지_않는다() {
        List<String> calls = new ArrayList<String>();
        AdminService service = service("hunter2", calls);

        assertNotNull(service.login(EXISTING_ID, "hunter2"));
        assertFalse(calls.contains("updatePassword"));
    }

    // ========================================

    /** login 을 여러 번 돌려 가장 빨랐던 시간(ns) */
    private static long fastestLoginNanos(AdminService service, String loginId) {
        long fastest = Long.MAX_VALUE;
        for (int i = 0; i < 5; i++) {
            long started = System.nanoTime();
            service.login(loginId, WRONG_PASSWORD);
            fastest = Math.min(fastest, System.nanoTime() - started);
        }
        return fastest;
    }

    private static AdminService service(String password) {
        return service(password, new ArrayList<String>());
    }

    private static AdminService service(String password, List<String> calls) {
        return serviceWithHash(PasswordUtil.encode(password), calls);
    }

    /** EXISTING_ID 하나만 알고 있는 가짜 매퍼를 꽂은 서비스 */
    private static AdminService serviceWithHash(String storedHash, List<String> calls) {
        AdminMapper mapper = (AdminMapper) Proxy.newProxyInstance(
                AdminMapper.class.getClassLoader(),
                new Class<?>[] { AdminMapper.class },
                (proxy, method, args) -> {
                    String name = method.getName();
                    calls.add(name);

                    if ("selectAdminByLoginId".equals(name)) {
                        if (!EXISTING_ID.equals(args[0])) {
                            return null;
                        }
                        AdminVO admin = new AdminVO();
                        admin.setAdminId(1L);
                        admin.setAdminLoginId(EXISTING_ID);
                        admin.setAdminPassword(storedHash);
                        admin.setAdminName("관리자");
                        return admin;
                    }
                    return method.getReturnType() == int.class ? 1 : null;
                });

        AdminServiceImpl service = new AdminServiceImpl();
        try {
            Field field = AdminServiceImpl.class.getDeclaredField("adminMapper");
            field.setAccessible(true);
            field.set(service, mapper);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("adminMapper 필드를 찾지 못했다", e);
        }
        return service;
    }

    /** 옛 구현이 만들던 값 그대로 — salt:hash, SHA-256 1회 */
    private static String legacyEncode(String password) throws Exception {
        byte[] salt = new byte[16];
        new java.security.SecureRandom().nextBytes(salt);
        String saltStr = java.util.Base64.getEncoder().encodeToString(salt);

        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
        md.update(saltStr.getBytes());
        return saltStr + ":" + java.util.Base64.getEncoder().encodeToString(md.digest(password.getBytes()));
    }
}

package com.irion.common.filter;

import org.junit.Test;

import javax.servlet.http.HttpSession;

import static org.junit.Assert.*;

/**
 * 관리자 영역 인증 필터.
 *
 *   1. 공개 경로 세 개만 열려 있고 나머지는 세션을 요구하는가
 *   2. 경로를 비틀어 공개 경로처럼 보이게 만들 수 없는가
 *   3. 막을 때 AJAX 에는 401, 브라우저에는 리다이렉트를 주는가
 */
public class AdminLoginFilterTest {

    private static final String LOGIN_PAGE = "/admin/admin-login.html";

    // ========================================
    // 공개 경로
    // ========================================

    @Test
    public void 로그인_페이지는_세션_없이_열린다() throws Exception {
        assertPassed(request(LOGIN_PAGE).browser(), null);
    }

    @Test
    public void 로그인_처리는_세션_없이_열린다() throws Exception {
        assertPassed(request("/admin/loginProc").method("POST"), null);
    }

    @Test
    public void 로그인_리다이렉트_경로도_열린다() throws Exception {
        assertPassed(request("/admin/admin-login").browser(), null);
    }

    @Test
    public void 관리자_밖의_경로는_상관하지_않는다() throws Exception {
        assertPassed(request("/schedule/list").browser(), null);
        assertPassed(request("/").browser(), null);
        assertPassed(request("/live/status").ajax(), null);
    }

    // ========================================
    // 보호 경로
    // ========================================

    @Test
    public void 세션이_없으면_로그인_화면으로_보낸다() throws Exception {
        FakeHttp.Response response = run(request("/admin/admin-schedule.html").browser(), null, false);

        assertEquals(302, response.status);
        assertEquals(LOGIN_PAGE, response.redirect);
    }

    @Test
    public void 세션은_있지만_로그인_전이면_막는다() throws Exception {
        FakeHttp.Response response =
                run(request("/admin/admin-schedule.html").browser(), FakeHttp.session(), false);

        assertEquals(302, response.status);
        assertEquals(LOGIN_PAGE, response.redirect);
    }

    @Test
    public void 로그인된_세션이면_통과시킨다() throws Exception {
        assertPassed(request("/admin/admin-schedule.html").browser(), FakeHttp.loggedIn());
        assertPassed(request("/admin/schedule/list").ajax(), FakeHttp.loggedIn());
    }

    @Test
    public void 컨텍스트_경로가_있어도_같은_판정을_한다() throws Exception {
        FakeHttp.Response response = run(
                request("/yeti/admin/admin-schedule.html").contextPath("/yeti").browser(), null, false);

        assertEquals(302, response.status);
        assertEquals("컨텍스트 경로를 붙여 돌려보내야 한다", "/yeti" + LOGIN_PAGE, response.redirect);
    }

    // ========================================
    // 경로를 비틀어 들어오는 요청
    // ========================================

    /**
     * 아래 경로들이 컨테이너에서 실제로 매핑되는 대상은 admin-schedule.html 이다.
     *
     * 정규화된 경로를 보는지, 화이트리스트를 정확히 일치로 따지는지 둘 다 본다.
     * 부분 문자열 판정으로 되돌리면 6개, 정규화만 빼면 2개가 깨진다.
     */
    @Test
    public void 상위_경로_기호로_우회할_수_없다() throws Exception {
        assertBlocked("/admin/loginProc/../admin-schedule.html");
        assertBlocked("/admin/admin-login.html/../admin-schedule.html");
    }

    @Test
    public void 퍼센트_인코딩으로_우회할_수_없다() throws Exception {
        assertBlocked("/admin/loginProc/..%2Fadmin-schedule.html");
    }

    @Test
    public void 중복_슬래시나_현재_경로_기호로_우회할_수_없다() throws Exception {
        assertBlocked("//admin//admin-schedule.html");
        assertBlocked("/admin/./admin-schedule.html");
        assertBlocked("/./admin/admin-schedule.html");
    }

    @Test
    public void 역슬래시로_우회할_수_없다() throws Exception {
        assertBlocked("/admin/loginProc/..\\admin-schedule.html");
    }

    /** 반대 방향 — 공개 경로에 뭔가 덧붙인 것은 공개가 아니다 */
    @Test
    public void 공개_경로에_덧붙인_것은_공개가_아니다() throws Exception {
        assertBlocked("/admin/loginProc-backup");
        assertBlocked("/admin/admin-login.html.bak");
        assertBlocked("/admin/loginProc/extra");
    }

    // ========================================
    // 막는 방식
    // ========================================

    /** jQuery 가 302 를 따라가 200 을 받으면 화면은 세션이 끊긴 것을 모른다 */
    @Test
    public void AJAX_요청에는_401_JSON_을_준다() throws Exception {
        FakeHttp.Response response = run(request("/admin/schedule/list").ajax(), null, false);

        assertEquals(401, response.status);
        assertNull("리다이렉트하면 안 된다", response.redirect);
        assertTrue(response.contentType.contains("application/json"));
        assertTrue(response.body().contains("로그인이 필요합니다"));
    }

    @Test
    public void 헤더가_없어도_JSON_요청이면_401_로_본다() throws Exception {
        // X-Requested-With 를 빠뜨린 호출도 놓치지 않는다
        FakeHttp.Response response = run(
                request("/admin/schedule").method("POST").contentType("application/json;charset=UTF-8"),
                null, false);

        assertEquals(401, response.status);
    }

    @Test
    public void 필터가_세션을_새로_만들지_않는다() throws Exception {
        FakeHttp.Request request = request("/admin/admin-schedule.html").browser();
        run(request, null, false);

        assertFalse("빈 세션이 쌓이면 안 된다", request.sessionCreated);
    }

    // ========================================

    private static FakeHttp.Request request(String uri) {
        return new FakeHttp.Request().uri(uri);
    }

    /** 통과해야 하는 요청 */
    private static void assertPassed(FakeHttp.Request request, HttpSession session) throws Exception {
        run(request, session, true);
    }

    /** 로그인 없이 접근할 수 없어야 하는 경로 */
    private static void assertBlocked(String uri) throws Exception {
        FakeHttp.Request request = request(uri).browser();
        FakeHttp.Response response = run(request, null, false);

        assertEquals(uri + " 가 통과했다", 302, response.status);
        assertEquals(LOGIN_PAGE, response.redirect);
    }

    private static FakeHttp.Response run(FakeHttp.Request request, HttpSession session,
                                         boolean expectPassed) throws Exception {
        if (session != null) {
            request.session(session);
        }
        FakeHttp.Response response = new FakeHttp.Response();
        FakeHttp.Chain chain = new FakeHttp.Chain();

        new AdminLoginFilter().doFilter(request.build(), response.build(), chain.build());

        assertEquals(expectPassed ? "통과했어야 한다" : "통과하면 안 된다",
                expectPassed, chain.passed);
        return response;
    }
}

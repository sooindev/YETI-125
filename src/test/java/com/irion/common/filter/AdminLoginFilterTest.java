package com.irion.common.filter;

import org.junit.Test;

import javax.servlet.http.HttpSession;

import static org.junit.Assert.*;

/**
 * 관리자 영역 인증 필터.
 *
 * 이 필터는 DispatcherServlet 앞에서 돌면서 /admin/* 를 통째로 막는다.
 * 정적 HTML 까지 가려주는 자리라, 여기가 뚫리면 관리자 화면이 그대로
 * 열린다. 확인해야 할 것은 세 가지다.
 *
 *   1. 공개 경로 세 개만 열려 있고 나머지는 세션을 요구하는가
 *   2. 경로를 비틀어 공개 경로처럼 보이게 만들 수 없는가
 *   3. 막을 때 AJAX 에는 401, 브라우저에는 리다이렉트를 주는가
 *
 * 2번이 이 필터가 존재하는 이유다. 예전에는 uri.contains("/admin/login")
 * 으로 걸렀는데, getRequestURI() 는 정규화 전 원본이라
 * /admin/login/../admin-schedule.html 같은 요청이 "포함하니까 공개" 로
 * 통과해 버렸다.
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
     * 셋 다 실제로 서블릿 컨테이너가 매핑하는 대상은
     * /admin/admin-schedule.html 이다.
     *
     * 두 가지를 동시에 확인한다 — 원본이 아니라 정규화된 경로를 보는가,
     * 그리고 화이트리스트를 부분 문자열이 아니라 정확히 일치로 따지는가.
     *
     * 예전 구현(uri.contains("/admin/login"))을 되살려 돌려보면 이 클래스에서
     * 6개가 깨진다: 상위 경로 · 퍼센트 인코딩 · 역슬래시 우회 3개가 뚫리고,
     * "공개 경로에 덧붙인 것" 도 통과하며, 반대로 정상 로그인 경로 2개는
     * 오히려 막힌다.
     *
     * 정규화만 빼고 정확히 일치는 남겨두면 대신 다른 둘이 깨진다 —
     * 중복 슬래시(//admin//...)와 컨텍스트 경로(/yeti/admin/...)가
     * startsWith("/admin/") 에 걸리지 않아 그대로 통과한다.
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

    /**
     * 반대 방향도 확인한다. 공개 경로에 뭔가 덧붙인 것은 공개가 아니다.
     * 화이트리스트가 contains 가 아니라 정확히 일치로 동작해야 한다.
     */
    @Test
    public void 공개_경로에_덧붙인_것은_공개가_아니다() throws Exception {
        assertBlocked("/admin/loginProc-backup");
        assertBlocked("/admin/admin-login.html.bak");
        assertBlocked("/admin/loginProc/extra");
    }

    // ========================================
    // 막는 방식
    // ========================================

    /**
     * AJAX 에 리다이렉트를 주면 안 된다.
     *
     * jQuery 는 302 를 따라가 로그인 HTML 을 200 으로 받는다. JSON 파싱에
     * 실패해 error 콜백으로 오지만 그 때 xhr.status 는 200 이라, 화면은
     * 로그인이 끊긴 것을 알아채지 못하고 조용히 빈 채로 남는다.
     */
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

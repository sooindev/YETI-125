package com.irion.common.web.filter;

import com.irion.testsupport.FakeHttp;

import org.junit.Test;

import javax.servlet.http.HttpSession;

import static org.junit.Assert.*;

/** 관리자 인증 필터 — 공개 경로만 개방, 경로 우회 차단, 차단 방식 확인 */
public class AdminLoginFilterTest {

    private static final String LOGIN_PAGE = "/admin/admin-login";


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


    /** 정규화 경로 사용 여부와 화이트리스트 정확 일치 여부 */
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
     * 톰캣은 /admin;x=1/schedule 을 이 필터로 넘기고 스프링도 ';x=1' 을 떼고 관리자로 보낸다.
     * 필터만 다른 주소로 보면 인증을 건너뛴 채 관리자 화면까지 간다
     */
    @Test
    public void 경로_파라미터로_우회할_수_없다() throws Exception {
        assertBlocked("/admin;x=1/schedule");
        assertBlocked("/admin/schedule;jsessionid=ABC123");
        assertBlocked("/admin;x=1/loginProc/../admin-schedule.html");
    }

    /** /admin 은 관리자 첫 화면 — 여기도 로그인 후에만 */
    @Test
    public void 슬래시_없는_admin_도_막는다() throws Exception {
        assertBlocked("/admin");
    }

    /** 반대 방향 — 공개 경로에 덧붙인 것은 공개가 아니다 */
    @Test
    public void 공개_경로에_덧붙인_것은_공개가_아니다() throws Exception {
        assertBlocked("/admin/loginProc-backup");
        assertBlocked("/admin/admin-login.html.bak");
        assertBlocked("/admin/loginProc/extra");
    }


    /** jQuery 가 302 를 따라가 200 을 받으면 화면은 세션 만료를 모른다 */
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
        // X-Requested-With 없는 호출도 감지
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

    /** 인증 없이 접근 불가여야 하는 경로 */
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

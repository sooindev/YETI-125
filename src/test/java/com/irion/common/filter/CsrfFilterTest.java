package com.irion.common.filter;

import com.irion.common.util.CsrfTokens;
import org.junit.Test;

import javax.servlet.http.HttpSession;

import static org.junit.Assert.*;

/**
 * 관리자 영역 CSRF 방어.
 *
 * 예전에는 관리자 API 가 contentType: application/json 을 쓴다는 점이
 * 우연히 방패 노릇을 하고 있었다 — 폼이나 &lt;img&gt; 로는 그 Content-Type 을
 * 만들 수 없기 때문이다. 하지만 그건 방어가 아니라 부작용이라, 요청
 * 하나만 단순 폼으로 바뀌어도 그대로 뚫린다.
 *
 * 확인할 것.
 *
 *   1. 상태를 바꾸는 메서드만 검사하고 조회는 그냥 통과시키는가
 *   2. 토큰이 없거나 틀리면 막는가
 *   3. 로그인 요청만 예외로 두되, 그 예외를 경로 조작으로 빌려쓸 수 없는가
 */
public class CsrfFilterTest {

    // ========================================
    // 검사 대상
    // ========================================

    @Test
    public void 조회는_토큰_없이_통과한다() throws Exception {
        assertPassed(request("/admin/schedule/list").method("GET"));
        assertPassed(request("/admin/csrf-token").method("HEAD"));
    }

    @Test
    public void 상태를_바꾸는_메서드는_토큰이_없으면_막는다() throws Exception {
        for (String method : new String[] { "POST", "PUT", "DELETE", "PATCH" }) {
            FakeHttp.Response response = run(request("/admin/schedule/7").method(method), false);
            assertEquals(method + " 가 통과했다", 403, response.status);
        }
    }

    @Test
    public void 올바른_토큰을_헤더로_보내면_통과한다() throws Exception {
        HttpSession session = FakeHttp.loggedIn();
        String token = CsrfTokens.issue(session);

        assertPassed(request("/admin/schedule").method("POST")
                .session(session)
                .header(CsrfTokens.HEADER, token));
    }

    @Test
    public void 올바른_토큰을_파라미터로_보내도_통과한다() throws Exception {
        HttpSession session = FakeHttp.loggedIn();
        String token = CsrfTokens.issue(session);

        assertPassed(request("/admin/schedule").method("POST")
                .session(session)
                .param("_csrf", token));
    }

    @Test
    public void 틀린_토큰은_막는다() throws Exception {
        HttpSession session = FakeHttp.loggedIn();
        CsrfTokens.issue(session);

        FakeHttp.Response response = run(request("/admin/schedule").method("POST")
                .session(session)
                .header(CsrfTokens.HEADER, "남의-토큰"), false);

        assertEquals(403, response.status);
    }

    @Test
    public void 다른_세션의_토큰으로는_통과할_수_없다() throws Exception {
        String otherToken = CsrfTokens.issue(FakeHttp.loggedIn());
        HttpSession mySession = FakeHttp.loggedIn();
        CsrfTokens.issue(mySession);

        FakeHttp.Response response = run(request("/admin/schedule").method("POST")
                .session(mySession)
                .header(CsrfTokens.HEADER, otherToken), false);

        assertEquals(403, response.status);
    }

    @Test
    public void 세션이_없으면_토큰을_들고_와도_막는다() throws Exception {
        FakeHttp.Response response = run(request("/admin/schedule").method("POST")
                .header(CsrfTokens.HEADER, "아무-토큰"), false);

        assertEquals(403, response.status);
    }

    @Test
    public void 빈_토큰은_토큰이_아니다() throws Exception {
        HttpSession session = FakeHttp.loggedIn();
        CsrfTokens.issue(session);

        FakeHttp.Response response = run(request("/admin/schedule").method("POST")
                .session(session)
                .header(CsrfTokens.HEADER, ""), false);

        assertEquals(403, response.status);
    }

    // ========================================
    // 로그인 예외
    // ========================================

    /** 로그인 시점에는 아직 세션이 없어 토큰을 줄 수가 없다 */
    @Test
    public void 로그인_처리는_예외로_통과시킨다() throws Exception {
        assertPassed(request("/admin/loginProc").method("POST"));
    }

    /**
     * 예외 경로를 빌려 다른 요청을 통과시킬 수 없어야 한다.
     *
     * /admin/loginProc/../schedule 이 서블릿 컨테이너에 닿는 실제 대상은
     * /admin/schedule 이다. 예외 판정을 원본 문자열로 하면 여기서 뚫린다.
     */
    @Test
    public void 예외_경로를_빌려_다른_요청을_통과시킬_수_없다() throws Exception {
        FakeHttp.Response response =
                run(request("/admin/loginProc/../schedule").method("POST"), false);

        assertEquals(403, response.status);
    }

    @Test
    public void 예외_경로에_덧붙인_것은_예외가_아니다() throws Exception {
        assertEquals(403, run(request("/admin/loginProc-x").method("POST"), false).status);
        assertEquals(403, run(request("/admin/loginProc/extra").method("POST"), false).status);
    }

    /** 반대로, 경로를 비틀어 들어와도 진짜 loginProc 이면 예외가 유지된다 */
    @Test
    public void 비틀린_경로라도_정규화_결과가_로그인이면_통과한다() throws Exception {
        assertPassed(request("/admin/schedule/../loginProc").method("POST"));
    }

    // ========================================
    // 막는 방식
    // ========================================

    @Test
    public void AJAX_에는_JSON_으로_알려준다() throws Exception {
        FakeHttp.Response response = run(request("/admin/schedule").method("POST").ajax(), false);

        assertEquals(403, response.status);
        assertTrue(response.contentType.contains("application/json"));
        assertTrue(response.body().contains("요청이 만료되었습니다"));
    }

    @Test
    public void 브라우저_요청에는_평문으로_알려준다() throws Exception {
        FakeHttp.Response response = run(request("/admin/schedule").method("POST").browser(), false);

        assertEquals(403, response.status);
        assertTrue(response.contentType.contains("text/plain"));
        assertEquals("CSRF token mismatch", response.body());
    }

    /**
     * 폼으로 위장한 요청도 막힌다.
     *
     * 이 필터를 만든 이유가 이것이다. Content-Type 이 폼이면 예전의
     * "우연한 방패" 는 통하지 않는다.
     */
    @Test
    public void 폼으로_위장한_요청도_막는다() throws Exception {
        FakeHttp.Response response = run(request("/admin/schedule")
                .method("POST")
                .contentType("application/x-www-form-urlencoded")
                .browser(), false);

        assertEquals(403, response.status);
    }

    // ========================================

    private static FakeHttp.Request request(String uri) {
        return new FakeHttp.Request().uri(uri);
    }

    private static void assertPassed(FakeHttp.Request request) throws Exception {
        run(request, true);
    }

    private static FakeHttp.Response run(FakeHttp.Request request, boolean expectPassed)
            throws Exception {
        FakeHttp.Response response = new FakeHttp.Response();
        FakeHttp.Chain chain = new FakeHttp.Chain();

        new CsrfFilter().doFilter(request.build(), response.build(), chain.build());

        assertEquals(expectPassed ? "통과했어야 한다" : "통과하면 안 된다",
                expectPassed, chain.passed);
        return response;
    }
}

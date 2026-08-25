package com.irion.common.filter;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** 옛 주소가 정규 주소로 301 되는지. 301 은 되돌리기 어려우므로 목적지를 못 박아둔다. */
public class LegacyHtmlRedirectFilterTest {

    @Test
    public void 옛_주소는_301_로_정규_주소를_가리킨다() throws Exception {
        assertMoved("/index.html", "/");
        assertMoved("/info.html", "/info");
        assertMoved("/schedule.html", "/schedule");
    }

    @Test
    public void 관리자_주소도_같이_옮긴다() throws Exception {
        assertMoved("/admin/admin-login.html", "/admin/admin-login");
        assertMoved("/admin/admin-schedule.html", "/admin/schedule");
    }

    /** 유입 경로를 잃지 않으려면 utm 같은 파라미터가 살아 있어야 한다 */
    @Test
    public void 쿼리스트링은_그대로_넘긴다() throws Exception {
        FakeHttp.Response response = run(
                new FakeHttp.Request().uri("/schedule.html").queryString("utm_source=x&y=2"));

        assertEquals("/schedule?utm_source=x&y=2", response.header("Location"));
    }

    /** ROOT 가 아닌 컨텍스트에 배포해도 주소가 깨지면 안 된다 */
    @Test
    public void 컨텍스트_경로를_붙여준다() throws Exception {
        FakeHttp.Response response = run(
                new FakeHttp.Request().uri("/yeti/schedule.html").contextPath("/yeti"));

        assertEquals(301, response.status);
        assertEquals("/yeti/schedule", response.header("Location"));
    }

    /** 정규 주소까지 건드리면 301 과 forward 가 서로를 부르며 끝없이 돈다. */
    @Test
    public void 정규_주소는_건드리지_않는다() throws Exception {
        assertPassed("/");
        assertPassed("/info");
        assertPassed("/schedule");
        assertPassed("/admin/schedule");
    }

    /** 톰캣이 /./ 와 /../ 는 정리해주지만 겹친 슬래시와 퍼센트 인코딩은 그대로 넘어온다. */
    @Test
    public void 다른_표기로도_옛_주소에_닿을_수_없다() throws Exception {
        assertMoved("//schedule.html", "/schedule");
        assertMoved("/schedule%2Ehtml", "/schedule");
        assertMoved("//admin//admin-schedule.html", "/admin/schedule");
    }

    /** 같은 곳을 가리키는 표기는 정규 표기 하나로 모은다 */
    @Test
    public void 겹친_슬래시와_끝_슬래시를_정리한다() throws Exception {
        assertMoved("/schedule/", "/schedule");
        assertMoved("/schedule//", "/schedule");
        assertMoved("//schedule", "/schedule");
        assertMoved("//info", "/info");
        assertMoved("//", "/");
    }

    /** 루트는 끝 슬래시가 정규 표기다 — 떼면 빈 주소가 된다 */
    @Test
    public void 루트는_그대로_둔다() throws Exception {
        assertPassed("/");
    }

    @Test
    public void 정적_파일과_API_는_지나간다() throws Exception {
        assertPassed("/resources/css/common.css");
        assertPassed("/resources/js/theme-init.js");
        assertPassed("/live/status");
        assertPassed("/sitemap.xml");
    }

    /** 목록에 없는 .html 은 옮긴 적이 없으니 404 로 가야 한다 */
    @Test
    public void 목록에_없는_html_은_지나간다() throws Exception {
        assertPassed("/nope.html");
        assertPassed("/admin/admin-login.html.bak");
    }

    // ========================================

    private static void assertMoved(String from, String to) throws Exception {
        FakeHttp.Response response = run(new FakeHttp.Request().uri(from));

        assertEquals(from + " 가 옮겨지지 않았다", 301, response.status);
        assertEquals(to, response.header("Location"));
        assertNull("301 은 sendRedirect(302) 가 아니다", response.redirect);
    }

    private static void assertPassed(String uri) throws Exception {
        FakeHttp.Chain chain = new FakeHttp.Chain();
        FakeHttp.Response response = new FakeHttp.Response();

        new LegacyHtmlRedirectFilter().doFilter(
                new FakeHttp.Request().uri(uri).build(), response.build(), chain.build());

        assertTrue(uri + " 가 막혔다", chain.passed);
        assertEquals(200, response.status);
    }

    private static FakeHttp.Response run(FakeHttp.Request request) throws Exception {
        FakeHttp.Chain chain = new FakeHttp.Chain();
        FakeHttp.Response response = new FakeHttp.Response();

        new LegacyHtmlRedirectFilter().doFilter(request.build(), response.build(), chain.build());

        assertFalse("옮긴 주소를 그대로 넘기면 두 주소가 모두 200 이 된다", chain.passed);
        return response;
    }
}

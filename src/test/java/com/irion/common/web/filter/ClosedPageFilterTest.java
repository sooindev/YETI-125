package com.irion.common.web.filter;

import com.irion.testsupport.FakeHttp;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 내려둔 방명록이 정말 닫혀 있는지.
 *
 * 메뉴에서 링크만 떼는 것은 숨기는 것이 아니다 — 주소를 아는 사람은 그대로 들어오고,
 * 글도 계속 쌓인다. 화면 · 목록 · 글쓰기가 한꺼번에 막히는 것을 여기서 못 박는다.
 */
public class ClosedPageFilterTest {

    @Test
    public void 방명록_화면은_홈으로_보낸다() throws Exception {
        FakeHttp.Response response = run(new FakeHttp.Request().uri("/guestbook"));

        assertEquals(302, response.status);
        assertEquals("/", response.redirect);
    }

    /** 301 로 보내면 되살린 뒤에도 브라우저가 한동안 홈으로 튄다 */
    @Test
    public void _301_이_아니라_302_로_보낸다() throws Exception {
        FakeHttp.Response response = run(new FakeHttp.Request().uri("/guestbook"));

        assertEquals(302, response.status);
    }

    /** 화면만 막으면 목록 API 로 글을 그대로 읽어 간다 */
    @Test
    public void 목록_API_도_함께_막는다() throws Exception {
        FakeHttp.Response response = run(new FakeHttp.Request().uri("/guestbook/list"));

        assertEquals(302, response.status);
    }

    /** 글쓰기는 홈으로 보낼 것이 없다 — 없는 주소로 답한다 */
    @Test
    public void 글쓰기는_404_로_막는다() throws Exception {
        FakeHttp.Response response = run(new FakeHttp.Request().uri("/guestbook").method("POST"));

        assertEquals(404, response.status);
    }

    /** 표기를 바꿔 비켜 가면 막은 것이 아니다 */
    @Test
    public void 다른_표기로도_들어올_수_없다() throws Exception {
        assertEquals(302, run(new FakeHttp.Request().uri("/guestbook;x=1")).status);
        assertEquals(302, run(new FakeHttp.Request().uri("/guestbook/../guestbook/list")).status);
        assertEquals(302, run(new FakeHttp.Request().uri("/guestbook%2Flist")).status);
    }

    /** ROOT 가 아닌 컨텍스트에 올려도 판정과 목적지가 함께 따라가야 한다 */
    @Test
    public void 컨텍스트_경로를_붙여준다() throws Exception {
        FakeHttp.Response response = run(
                new FakeHttp.Request().uri("/yeti/guestbook").contextPath("/yeti"));

        assertEquals(302, response.status);
        assertEquals("/yeti/", response.redirect);
    }

    /** 관리자 삭제는 /admin 아래라 이 필터와 무관하다 — 데이터 관리는 계속 된다 */
    @Test
    public void 관리자_방명록_주소는_지나간다() throws Exception {
        assertPassed("/admin/guestbook/1");
    }

    /** 이름이 겹치는 다른 주소까지 끌려 들어가면 안 된다 */
    @Test
    public void 다른_화면은_지나간다() throws Exception {
        assertPassed("/");
        assertPassed("/schedule");
        assertPassed("/info");
        assertPassed("/guestbooking");
        assertPassed("/resources/css/base/common.css");
    }

    // ========================================

    private static FakeHttp.Response run(FakeHttp.Request request) throws Exception {
        FakeHttp.Chain chain = new FakeHttp.Chain();
        FakeHttp.Response response = new FakeHttp.Response();

        new ClosedPageFilter().doFilter(request.build(), response.build(), chain.build());

        assertFalse("막은 주소를 그대로 넘기면 화면이 그대로 뜬다", chain.passed);
        return response;
    }

    private static void assertPassed(String uri) throws Exception {
        FakeHttp.Chain chain = new FakeHttp.Chain();
        FakeHttp.Response response = new FakeHttp.Response();

        new ClosedPageFilter().doFilter(
                new FakeHttp.Request().uri(uri).build(), response.build(), chain.build());

        assertTrue(uri + " 가 막혔다", chain.passed);
        assertEquals(200, response.status);
    }
}

package com.irion.common.web.filter;

import com.irion.testsupport.FakeHttp;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 로그인 빈도 제한 필터 — 로그인 POST 만 세는가, 한도를 넘으면 해시 계산 앞에서 끊는가,
 * 헤더를 지어내 우회할 수 없는가.
 *
 * 한도(20회/분)는 상수라 테스트에서 바꿀 수 없다. 대신 그 횟수만큼 실제로 두드린다 —
 * 필터가 무엇을 세는 요청으로 보는지가 여기서 확인하고 싶은 것이다.
 */
public class LoginRateLimitFilterTest {

    /** LoginRateLimiter 의 기본 한도. 넘겨야 막히는 것을 보려면 여기서도 알아야 한다 */
    private static final int LIMIT = 20;

    @Test
    public void 한도까지는_통과한다() throws Exception {
        LoginRateLimitFilter filter = new LoginRateLimitFilter();

        for (int i = 0; i < LIMIT; i++) {
            FakeHttp.Chain chain = new FakeHttp.Chain();
            run(filter, loginRequest("1.2.3.4"), chain);
            assertTrue((i + 1) + "번째가 막혔다", chain.passed);
        }
    }

    @Test
    public void 한도를_넘으면_막고_429_와_안내를_준다() throws Exception {
        LoginRateLimitFilter filter = new LoginRateLimitFilter();

        for (int i = 0; i < LIMIT; i++) {
            run(filter, loginRequest("1.2.3.4"), new FakeHttp.Chain());
        }

        FakeHttp.Chain chain = new FakeHttp.Chain();
        FakeHttp.Response response = run(filter, loginRequest("1.2.3.4"), chain);

        assertFalse("컨트롤러까지 가면 안 된다 — 여기서 끊는 이유가 해시 계산이다", chain.passed);
        assertEquals(429, response.status);
        assertNotNull("Retry-After 가 없다", response.header("Retry-After"));
        assertTrue("JSON 이어야 화면이 문구를 읽는다: " + response.contentType,
                response.contentType.contains("application/json"));
        assertTrue("안내 문구가 없다: " + response.body(),
                response.body().contains("초 후 다시 시도해 주세요"));
    }

    @Test
    public void 주소가_다르면_따로_센다() throws Exception {
        LoginRateLimitFilter filter = new LoginRateLimitFilter();

        for (int i = 0; i < LIMIT + 5; i++) {
            run(filter, loginRequest("1.2.3.4"), new FakeHttp.Chain());
        }

        FakeHttp.Chain chain = new FakeHttp.Chain();
        run(filter, loginRequest("5.6.7.8"), chain);

        assertTrue("옆집이 막혔다고 같이 막히면 안 된다", chain.passed);
    }

    /** 헤더는 보낸 쪽이 지어낼 수 있다. 앞에 뭘 적어 보내도 nginx 가 덧붙인 마지막 값으로 센다 */
    @Test
    public void X_Forwarded_For_를_지어내도_우회할_수_없다() throws Exception {
        LoginRateLimitFilter filter = new LoginRateLimitFilter();

        for (int i = 0; i < LIMIT; i++) {
            FakeHttp.Request request = new FakeHttp.Request()
                    .uri("/admin/loginProc").method("POST").ajax()
                    .remoteAddr("127.0.0.1")
                    .header("X-Forwarded-For", "10.0.0." + i + ", 1.2.3.4");
            run(filter, request, new FakeHttp.Chain());
        }

        FakeHttp.Chain chain = new FakeHttp.Chain();
        FakeHttp.Request request = new FakeHttp.Request()
                .uri("/admin/loginProc").method("POST").ajax()
                .remoteAddr("127.0.0.1")
                .header("X-Forwarded-For", "8.8.8.8, 1.2.3.4");
        run(filter, request, chain);

        assertFalse("앞쪽 값을 바꿔가며 보내면 뚫리는 상태다", chain.passed);
    }

    @Test
    public void 로그인이_아닌_요청은_세지_않는다() throws Exception {
        LoginRateLimitFilter filter = new LoginRateLimitFilter();

        for (int i = 0; i < LIMIT * 3; i++) {
            FakeHttp.Request request = new FakeHttp.Request()
                    .uri("/admin/schedule").method("POST").ajax().remoteAddr("1.2.3.4");
            FakeHttp.Chain chain = new FakeHttp.Chain();
            run(filter, request, chain);
            assertTrue("관리자 화면의 다른 요청까지 막으면 안 된다", chain.passed);
        }
    }

    @Test
    public void 로그인_화면을_여는_GET_은_세지_않는다() throws Exception {
        LoginRateLimitFilter filter = new LoginRateLimitFilter();

        for (int i = 0; i < LIMIT * 3; i++) {
            FakeHttp.Request request = new FakeHttp.Request()
                    .uri("/admin/loginProc").method("GET").browser().remoteAddr("1.2.3.4");
            FakeHttp.Chain chain = new FakeHttp.Chain();
            run(filter, request, chain);
            assertTrue("비용이 드는 것은 POST 다", chain.passed);
        }
    }

    /** 원본 주소로 판정하면 이 요청이 검사를 비켜 간다 */
    @Test
    public void 경로를_비틀어도_같은_요청으로_센다() throws Exception {
        LoginRateLimitFilter filter = new LoginRateLimitFilter();

        for (int i = 0; i < LIMIT; i++) {
            run(filter, loginRequest("1.2.3.4"), new FakeHttp.Chain());
        }

        FakeHttp.Chain chain = new FakeHttp.Chain();
        FakeHttp.Request twisted = new FakeHttp.Request()
                .uri("/admin/schedule/../loginProc").method("POST").ajax()
                .remoteAddr("1.2.3.4");
        run(filter, twisted, chain);

        assertFalse("정규화 전 경로로 보면 여기서 뚫린다", chain.passed);
    }

    @Test
    public void 브라우저_요청에는_JSON_대신_글로_답한다() throws Exception {
        LoginRateLimitFilter filter = new LoginRateLimitFilter();

        for (int i = 0; i < LIMIT; i++) {
            run(filter, loginRequest("1.2.3.4"), new FakeHttp.Chain());
        }

        FakeHttp.Request request = new FakeHttp.Request()
                .uri("/admin/loginProc").method("POST").browser().remoteAddr("1.2.3.4");
        FakeHttp.Response response = run(filter, request, new FakeHttp.Chain());

        assertEquals(429, response.status);
        assertTrue("HTML 을 기대한 요청이다: " + response.contentType,
                response.contentType.contains("text/plain"));
    }

    @Test
    public void 로그에_주소를_통째로_남기지_않는다() {
        assertEquals("1.2.3.x", LoginRateLimitFilter.mask("1.2.3.4"));
        assertEquals("2001:db8:x", LoginRateLimitFilter.mask("2001:db8::1"));
        assertEquals("(unknown)", LoginRateLimitFilter.mask(""));
        assertEquals("(unknown)", LoginRateLimitFilter.mask(null));
    }


    /** nginx 를 거쳐 들어온 로그인 요청 한 건 */
    private static FakeHttp.Request loginRequest(String clientIp) {
        return new FakeHttp.Request()
                .uri("/admin/loginProc").method("POST").ajax()
                .remoteAddr("127.0.0.1")
                .header("X-Forwarded-For", clientIp);
    }

    private static FakeHttp.Response run(LoginRateLimitFilter filter,
                                         FakeHttp.Request request,
                                         FakeHttp.Chain chain) throws Exception {
        FakeHttp.Response response = new FakeHttp.Response();
        filter.doFilter(request.build(), response.build(), chain.build());
        return response;
    }
}

package com.irion.common.interceptor;

import com.irion.admin.vo.AdminVO;
import com.irion.testsupport.FakeHttp;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * 인증 두 겹 중 안쪽. 바깥 겹인 AdminLoginFilter 와 같은 판정을 내려야 한다 —
 * 한쪽만 통과시키면 필터를 우회하는 경로가 생기고, 응답 모양이 갈리면 화면이 엇갈린다.
 */
public class AdminLoginInterceptorTest {

    private final AdminLoginInterceptor interceptor = new AdminLoginInterceptor();

    @Test
    public void 세션이_없으면_로그인_페이지로_보낸다() throws Exception {
        FakeHttp.Request request = new FakeHttp.Request().uri("/admin/schedule").browser();
        FakeHttp.Response response = new FakeHttp.Response();

        boolean proceed = interceptor.preHandle(request.build(), response.build(), null);

        assertFalse("로그인 전에는 컨트롤러로 넘기면 안 된다", proceed);
        assertEquals("/admin/admin-login", response.redirect);
    }

    /** 컨텍스트 경로가 붙은 배포에서도 주소가 맞아야 한다 */
    @Test
    public void 리다이렉트에_컨텍스트_경로를_붙인다() throws Exception {
        FakeHttp.Request request = new FakeHttp.Request()
                .uri("/yeti/admin/schedule").contextPath("/yeti").browser();
        FakeHttp.Response response = new FakeHttp.Response();

        interceptor.preHandle(request.build(), response.build(), null);

        assertEquals("/yeti/admin/admin-login", response.redirect);
    }

    /** 302 를 주면 jQuery 가 따라가 로그인 HTML 을 200 으로 받는다 — 필터와 같은 판정 */
    @Test
    public void AJAX_요청에는_401_을_준다() throws Exception {
        FakeHttp.Request request = new FakeHttp.Request().uri("/admin/schedule/list").ajax();
        FakeHttp.Response response = new FakeHttp.Response();

        boolean proceed = interceptor.preHandle(request.build(), response.build(), null);

        assertFalse(proceed);
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.status);
        assertNull("AJAX 에는 리다이렉트를 주면 안 된다", response.redirect);
        assertTrue("JSON 으로 돌려줘야 한다: " + response.contentType,
                response.contentType != null && response.contentType.contains("application/json"));
        assertTrue("실패 응답이어야 한다: " + response.body(),
                response.body().contains("\"success\":false"));
    }

    @Test
    public void 세션은_있지만_로그인_정보가_없으면_막는다() throws Exception {
        FakeHttp.Request request = new FakeHttp.Request()
                .uri("/admin/schedule").session(FakeHttp.session()).browser();
        FakeHttp.Response response = new FakeHttp.Response();

        assertFalse(interceptor.preHandle(request.build(), response.build(), null));
        assertEquals("/admin/admin-login", response.redirect);
    }

    @Test
    public void 로그인했으면_통과시킨다() throws Exception {
        FakeHttp.Request request = new FakeHttp.Request()
                .uri("/admin/schedule").session(adminSession()).browser();
        FakeHttp.Response response = new FakeHttp.Response();

        boolean proceed = interceptor.preHandle(request.build(), response.build(), null);

        assertTrue(proceed);
        assertNull(response.redirect);
        assertEquals(200, response.status);
    }

    /** getSession(true) 로 물으면 로그인하지 않은 요청마다 빈 세션이 쌓인다 */
    @Test
    public void 세션을_새로_만들지_않는다() throws Exception {
        FakeHttp.Request request = new FakeHttp.Request().uri("/admin/schedule").browser();
        HttpServletRequest built = request.build();

        interceptor.preHandle(built, new FakeHttp.Response().build(), null);

        assertFalse("로그인 검사가 세션을 만들면 안 된다", request.sessionCreated);
    }

    /** 세션에 든 값은 AdminVO 다 — 다른 타입이 들어오면 여기서 드러난다 */
    private static HttpSession adminSession() {
        AdminVO admin = new AdminVO();
        admin.setAdminId(1L);
        admin.setAdminLoginId("tester");

        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("adminUser", admin);
        return FakeHttp.session(attributes);
    }
}

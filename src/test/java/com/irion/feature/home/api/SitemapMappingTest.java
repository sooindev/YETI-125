package com.irion.feature.home.api;

import com.irion.testsupport.FakeHttp;
import org.junit.Test;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.context.support.StaticWebApplicationContext;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * "/sitemap.xml 이 정말 컨트롤러에 걸리는가."
 *
 * 운영 배포에서 사이트맵이 우리 컨트롤러의 출력과 다른 것이 나와, 매핑 자체를 의심했다.
 * 주소 끝의 <b>.xml 확장자</b>가 의심스러운 자리다 — 스프링은 예전에 확장자를 보고
 * 응답 형식을 협상했고(useSuffixPatternMatch · favorPathExtension), 그 동작에 걸리면
 * 확장자가 붙은 주소는 조용히 다른 곳으로 간다.
 *
 * 톰캣 없이 핸들러 매핑만 세워서 확인한다. 이 테스트가 통과하는 한, 사이트맵이
 * 이상하게 나오는 원인은 매핑이 아니라 그 바깥(배포본·서버 설정)에 있다.
 */
public class SitemapMappingTest {

    @Test
    public void 사이트맵_주소는_SitemapController_로_간다() throws Exception {
        HandlerMethod handler = handlerFor("/sitemap.xml");

        assertNotNull("/sitemap.xml 이 아무 핸들러에도 걸리지 않았다", handler);
        assertEquals(SitemapController.class, handler.getBeanType());
        assertEquals("sitemap", handler.getMethod().getName());
    }

    /** 확장자를 뗀 주소까지 덩달아 걸리면 안 된다 — 그런 주소는 없다 */
    @Test
    public void 확장자를_뗀_주소는_걸리지_않는다() throws Exception {
        assertEquals(null, handlerFor("/sitemap"));
    }

    /** 클립 상세도 같은 매핑을 쓰므로 함께 못 박는다 */
    @Test
    public void 클립_상세_주소는_정규식_모양만_받는다() throws Exception {
        assertNotNull(handlerFor("/clips/kJ3y4mwyqe"));
        assertNotNull(handlerFor("/clips"));

        assertEquals("점이 든 주소는 받지 않는다", null, handlerFor("/clips/a.jsp"));
        assertEquals("한글은 받지 않는다", null, handlerFor("/clips/한글"));
    }

    // ── 도우미 ────────────────────────────────────────────

    private static HandlerMethod handlerFor(String path) throws Exception {
        StaticWebApplicationContext context = new StaticWebApplicationContext();
        context.registerSingleton("sitemapController", SitemapController.class);
        context.registerSingleton("clipController", com.irion.feature.clips.api.ClipController.class);
        context.refresh();

        RequestMappingHandlerMapping mapping = new RequestMappingHandlerMapping();
        mapping.setApplicationContext(context);
        mapping.afterPropertiesSet();

        HttpServletRequest request = new FakeHttp.Request().uri(path).method("GET").build();
        HandlerExecutionChain chain = mapping.getHandler(request);

        if (chain == null) {
            return null;
        }
        assertTrue(chain.getHandler() instanceof HandlerMethod);
        return (HandlerMethod) chain.getHandler();
    }
}

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
 * /sitemap.xml 이 컨트롤러에 걸리는지.
 *
 * 운영에서 사이트맵 출력이 달라 매핑을 의심했다. 의심 지점은 .xml 확장자 —
 * 스프링이 확장자로 응답 형식을 협상하던 동작(useSuffixPatternMatch · favorPathExtension)에
 * 걸리면 확장자 붙은 주소가 조용히 다른 곳으로 간다.
 *
 * 톰캣 없이 핸들러 매핑만 세워 확인. 통과하는 한 원인은 매핑 바깥에 있다
 */
public class SitemapMappingTest {

    @Test
    public void 사이트맵_주소는_SitemapController_로_간다() throws Exception {
        HandlerMethod handler = handlerFor("/sitemap.xml");

        assertNotNull("/sitemap.xml 이 아무 핸들러에도 걸리지 않았다", handler);
        assertEquals(SitemapController.class, handler.getBeanType());
        assertEquals("sitemap", handler.getMethod().getName());
    }

    /** 확장자를 뗀 주소는 걸리면 안 된다 — 그런 주소는 없다 */
    @Test
    public void 확장자를_뗀_주소는_걸리지_않는다() throws Exception {
        assertEquals(null, handlerFor("/sitemap"));
    }

    /** 클립 상세도 같은 매핑이라 함께 검증 */
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

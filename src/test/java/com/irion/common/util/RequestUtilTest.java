package com.irion.common.util;

import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * 경로 정규화 / AJAX 판정.
 *
 * 예전 필터는 uri.contains("/admin/login") 으로 공개 경로를 가렸다.
 * getRequestURI() 는 정규화 전 원본이라 아래 케이스들이 뚫렸다.
 */
public class RequestUtilTest {

    @Test
    public void 평범한_경로는_그대로다() {
        assertEquals("/admin/admin-schedule.html",
                RequestUtil.normalizedPath(request("/admin/admin-schedule.html", "")));
    }

    @Test
    public void 컨텍스트_경로를_떼어낸다() {
        assertEquals("/admin/admin-login.html",
                RequestUtil.normalizedPath(request("/yeti/admin/admin-login.html", "/yeti")));
    }

    @Test
    public void 상위_경로_기호를_정리한다() {
        // 예전 필터는 이 요청을 "/admin/login 을 포함하니 공개" 로 통과시켰다
        assertEquals("/admin/admin-schedule.html",
                RequestUtil.normalizedPath(request("/admin/login/../admin-schedule.html", "")));
    }

    @Test
    public void 퍼센트_인코딩을_먼저_푼다() {
        assertEquals("/admin/admin-schedule.html",
                RequestUtil.normalizedPath(request("/admin/login/..%2Fadmin-schedule.html", "")));
    }

    @Test
    public void 현재_경로_기호와_중복_슬래시를_지운다() {
        assertEquals("/admin/admin-schedule.html",
                RequestUtil.normalizedPath(request("/admin/.//admin-schedule.html", "")));
    }

    @Test
    public void 루트를_넘어서지_않는다() {
        assertEquals("/etc/passwd",
                RequestUtil.normalizedPath(request("/../../../etc/passwd", "")));
    }

    @Test
    public void 빈_경로는_루트다() {
        assertEquals("/", RequestUtil.normalizedPath(request("/", "")));
    }

    // ── AJAX 판정 ────────────────────────────────

    @Test
    public void X_Requested_With_를_본다() {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("X-Requested-With", "XMLHttpRequest");

        assertTrue(RequestUtil.isAjaxRequest(request("/admin/schedule/list", "", headers, null)));
    }

    @Test
    public void JSON_본문도_AJAX_로_본다() {
        assertTrue(RequestUtil.isAjaxRequest(
                request("/admin/schedule", "", new HashMap<String, String>(), "application/json;charset=UTF-8")));
    }

    @Test
    public void JSON_만_받겠다는_Accept_도_AJAX_로_본다() {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Accept", "application/json, text/plain, */*");

        assertTrue(RequestUtil.isAjaxRequest(request("/admin/schedule/list", "", headers, null)));
    }

    @Test
    public void 주소창으로_들어온_요청은_AJAX_가_아니다() {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");

        assertFalse(RequestUtil.isAjaxRequest(request("/admin/admin-schedule.html", "", headers, null)));
    }

    @Test
    public void 헤더가_아무것도_없으면_AJAX_가_아니다() {
        assertFalse(RequestUtil.isAjaxRequest(
                request("/admin/admin-schedule.html", "", new HashMap<String, String>(), null)));
    }

    // ── 헬퍼 ─────────────────────────────────────

    private static HttpServletRequest request(String uri, String contextPath) {
        return request(uri, contextPath, new HashMap<String, String>(), null);
    }

    /** HttpServletRequest 는 메서드가 많아 동적 프록시로 필요한 것만 답한다 */
    private static HttpServletRequest request(final String uri, final String contextPath,
                                              final Map<String, String> headers, final String contentType) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                RequestUtilTest.class.getClassLoader(),
                new Class<?>[] { HttpServletRequest.class },
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        String name = method.getName();
                        if ("getRequestURI".equals(name)) return uri;
                        if ("getContextPath".equals(name)) return contextPath;
                        if ("getContentType".equals(name)) return contentType;
                        if ("getHeader".equals(name)) return headers.get(args[0]);
                        return null;
                    }
                });
    }
}

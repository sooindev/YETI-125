package com.irion.common.util;

import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/** 경로 정규화 / AJAX 판정. getRequestURI() 는 정규화 전 원본이라 contains 판정은 뚫린다. */
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

    /**
     * 톰캣도 스프링도 매핑 전에 경로 파라미터를 뗀다. 여기만 들고 있으면
     * "/admin/" 으로 시작하지 않는 것처럼 보여 인증 필터가 통째로 열린다.
     */
    @Test
    public void 경로_파라미터는_떼고_본다() {
        assertEquals("/admin/schedule",
                RequestUtil.normalizedPath(request("/admin;x=1/schedule", "")));
        assertEquals("/admin/schedule",
                RequestUtil.normalizedPath(request("/admin/schedule;jsessionid=ABC123", "")));
        assertEquals("/admin/schedule",
                RequestUtil.normalizedPath(request("/admin;a=1/schedule;b=2", "")));
    }

    /** 경로 파라미터를 떼고도 .. 는 그대로 눌러야 한다 */
    @Test
    public void 경로_파라미터와_상위이동이_겹쳐도_막는다() {
        assertEquals("/admin/admin-schedule.html",
                RequestUtil.normalizedPath(request("/admin;x=1/loginProc/../admin-schedule.html", "")));
    }

    /**
     * 떼는 시점은 디코딩보다 앞이다 — 톰캣이 그 순서다.
     * 뒤에 떼면 %3B 로 보낸 진짜 세미콜론까지 잘려 이번엔 반대로 어긋난다.
     */
    @Test
    public void 인코딩된_세미콜론은_파라미터가_아니다() {
        assertEquals("/admin/loginProc;x",
                RequestUtil.normalizedPath(request("/admin/loginProc%3Bx", "")));
    }

    @Test
    public void 세미콜론이_없으면_경로를_그대로_둔다() {
        assertEquals("/a/b/c", RequestUtil.stripPathParameters("/a/b/c"));
    }

    @Test
    public void 빈_경로는_루트다() {
        assertEquals("/", RequestUtil.normalizedPath(request("/", "")));
    }


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

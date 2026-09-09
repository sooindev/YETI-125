package com.irion.common.web;

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


    @Test
    public void 프록시를_거친_요청은_X_Forwarded_For_의_마지막_값을_쓴다() {
        // nginx 의 $proxy_add_x_forwarded_for 는 자기가 본 주소를 뒤에 덧붙인다
        assertEquals("203.0.113.9",
                RequestUtil.clientIp(incoming("127.0.0.1", "198.51.100.7, 203.0.113.9")));
    }

    @Test
    public void 값이_하나뿐이면_그것을_쓴다() {
        assertEquals("203.0.113.9", RequestUtil.clientIp(incoming("127.0.0.1", "203.0.113.9")));
    }

    /** 앞쪽 값은 보낸 쪽이 지어낸 것일 수 있다 — 그걸 쓰면 제한을 무한히 우회한다 */
    @Test
    public void 앞에_지어낸_값이_붙어도_마지막_값만_본다() {
        assertEquals("203.0.113.9",
                RequestUtil.clientIp(incoming("127.0.0.1", "9.9.9.9, 8.8.8.8, 203.0.113.9")));
    }

    /** 톰캣에 직접 닿은 요청이다. 헤더는 전부 보낸 쪽 말이라 믿지 않는다 */
    @Test
    public void 프록시를_거치지_않으면_헤더를_보지_않는다() {
        assertEquals("198.51.100.7",
                RequestUtil.clientIp(incoming("198.51.100.7", "1.1.1.1")));
    }

    @Test
    public void 헤더가_없으면_상대_주소를_쓴다() {
        assertEquals("127.0.0.1", RequestUtil.clientIp(incoming("127.0.0.1", null)));
    }

    @Test
    public void 헤더가_비어_있어도_상대_주소로_물러난다() {
        assertEquals("127.0.0.1", RequestUtil.clientIp(incoming("127.0.0.1", " , ")));
    }

    @Test
    public void IPv6_루프백도_프록시로_본다() {
        assertEquals("203.0.113.9", RequestUtil.clientIp(incoming("::1", "203.0.113.9")));
    }

    @Test
    public void 상대_주소를_모르면_빈_값이다() {
        assertEquals("", RequestUtil.clientIp(incoming(null, "203.0.113.9")));
    }


    /** clientIp 는 상대 주소와 X-Forwarded-For 두 가지만 본다 */
    private static HttpServletRequest incoming(final String remoteAddr, final String forwardedFor) {
        Map<String, String> headers = new HashMap<String, String>();
        if (forwardedFor != null) {
            headers.put("X-Forwarded-For", forwardedFor);
        }
        return request("/admin/loginProc", "", headers, null, remoteAddr);
    }

    private static HttpServletRequest request(String uri, String contextPath) {
        return request(uri, contextPath, new HashMap<String, String>(), null);
    }

    private static HttpServletRequest request(String uri, String contextPath,
                                              Map<String, String> headers, String contentType) {
        return request(uri, contextPath, headers, contentType, null);
    }

    /** HttpServletRequest 는 메서드가 많아 동적 프록시로 필요한 것만 답한다 */
    private static HttpServletRequest request(final String uri, final String contextPath,
                                              final Map<String, String> headers, final String contentType,
                                              final String remoteAddr) {
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
                        if ("getRemoteAddr".equals(name)) return remoteAddr;
                        return null;
                    }
                });
    }
}

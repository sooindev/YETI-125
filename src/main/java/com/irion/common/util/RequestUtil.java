package com.irion.common.util;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 요청 판별 도우미.
 *
 * AJAX 판정을 필터와 인터셉터 두 곳에서 쓴다. 서로 어긋나면 한쪽은 401,
 * 다른 쪽은 리다이렉트를 주게 되므로 한 곳에 모아둔다.
 */
public final class RequestUtil {

    private RequestUtil() {
    }

    /**
     * AJAX 요청인가.
     *
     * X-Requested-With 만 보면 헤더를 빠뜨린 호출을 놓치므로 Content-Type 과
     * Accept 도 함께 본다. 주소창 요청은 Accept 에 text/html 이 있어 걸리지 않는다.
     */
    public static boolean isAjaxRequest(HttpServletRequest request) {
        if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
            return true;
        }

        String contentType = request.getContentType();
        if (contentType != null && contentType.contains("application/json")) {
            return true;
        }

        String accept = request.getHeader("Accept");
        return accept != null
                && accept.contains("application/json")
                && !accept.contains("text/html");
    }

    /**
     * 컨텍스트 경로를 뗀, 정규화된 요청 경로. 항상 "/" 로 시작한다.
     *
     * getRequestURI() 는 퍼센트 인코딩과 .. 가 남은 원본이라, 그대로 인증 예외를
     * 판정하면 /admin/login/../admin-schedule.html 같은 요청에 뚫린다. 컨테이너가
     * 실제로 매핑하는 대상은 /admin/admin-schedule.html 이다.
     */
    public static String normalizedPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();

        String path = (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath))
                ? uri.substring(contextPath.length())
                : uri;

        try {
            path = URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException | IllegalArgumentException e) {
            // 디코딩할 수 없으면 원본 그대로 정규화한다
        }

        // 역슬래시를 구분자로 받아들이는 환경이 있다 (윈도우)
        path = path.replace('\\', '/');

        Deque<String> segments = new ArrayDeque<String>();
        for (String segment : path.split("/")) {
            if (segment.isEmpty() || ".".equals(segment)) {
                continue;
            }
            if ("..".equals(segment)) {
                segments.pollLast();
                continue;
            }
            segments.addLast(segment);
        }

        StringBuilder normalized = new StringBuilder();
        for (String segment : segments) {
            normalized.append('/').append(segment);
        }
        return normalized.length() == 0 ? "/" : normalized.toString();
    }
}

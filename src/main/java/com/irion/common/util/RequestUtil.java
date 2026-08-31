package com.irion.common.util;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayDeque;
import java.util.Deque;

/** 요청 판별. 필터와 인터셉터가 같은 판정을 써야 401 과 리다이렉트가 엇갈리지 않는다. */
public final class RequestUtil {

    private RequestUtil() {
    }

    /** AJAX 요청인가. X-Requested-With 만 보면 헤더를 빠뜨린 호출을 놓친다. */
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
     * 컨텍스트 경로를 뗀 정규화 경로. 항상 "/" 로 시작한다.
     * getRequestURI() 원본으로 인증 예외를 판정하면 /admin/x/../admin-schedule 같은 요청에 뚫린다.
     *
     * 톰캣이 필터를 고르고 스프링이 컨트롤러를 고를 때 쓰는 경로와 같은 모양이어야 한다.
     * 어긋나면 "필터는 딴 주소로 보고 통과시켰는데 스프링은 관리자 화면으로 보낸" 상태가 된다.
     */
    public static String normalizedPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();

        String path = (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath))
                ? uri.substring(contextPath.length())
                : uri;

        // 디코딩보다 먼저 떼야 한다 — 톰캣이 그 순서다.
        // 뒤에 떼면 %3B 로 보낸 진짜 세미콜론까지 잘라내 이번엔 반대로 어긋난다.
        path = stripPathParameters(path);

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

    /**
     * 경로 파라미터를 뗀다 — 세그먼트마다 ';' 부터 다음 '/' 까지.
     *
     * /admin;x=1/schedule 을 톰캣도 스프링도 /admin/schedule 로 읽는다.
     * 여기서만 ';x=1' 을 들고 있으면 "/admin/" 으로 시작하지 않는 것처럼 보여
     * 인증 검사를 통째로 건너뛴다.
     */
    static String stripPathParameters(String path) {
        int semicolon = path.indexOf(';');
        if (semicolon < 0) {
            return path;
        }

        StringBuilder stripped = new StringBuilder(path.length());
        int from = 0;

        while (semicolon >= 0) {
            stripped.append(path, from, semicolon);

            int slash = path.indexOf('/', semicolon);
            if (slash < 0) {
                // 마지막 세그먼트였다 — 뒤는 전부 파라미터다
                return stripped.toString();
            }

            from = slash;
            semicolon = path.indexOf(';', slash);
        }

        return stripped.append(path.substring(from)).toString();
    }
}

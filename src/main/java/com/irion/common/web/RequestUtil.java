package com.irion.common.web;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayDeque;
import java.util.Deque;

/** 요청 판별. 필터와 인터셉터가 같은 기준을 써야 401 과 리다이렉트가 엇갈리지 않음 */
public final class RequestUtil {

    private RequestUtil() {
    }

    /** AJAX 여부. X-Requested-With 만 보면 헤더 없는 호출을 놓침 */
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
     * 요청자 주소. 빈도 제한의 기준값.
     *
     * 헤더는 보낸 쪽이 지어낼 수 있다 — 통째로 믿으면 제한을 무한 우회하거나
     * 남의 주소를 적어 그 사람을 막을 수 있다. 그래서 두 가지만 신뢰한다.
     *
     *   1. 루프백 요청 — nginx 가 넘긴 것. 이때만 X-Forwarded-For 확인
     *   2. 그 헤더의 마지막 값 — nginx 가 자기가 본 주소를 뒤에 덧붙임
     *
     * 루프백이 아니면 톰캣에 직접 닿은 요청이라 헤더를 보지 않는다.
     * nginx 가 헤더를 안 붙이면 모든 요청이 루프백 하나로 묶인다(config/nginx 참고)
     */
    public static String clientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (remoteAddr == null || remoteAddr.trim().isEmpty()) {
            return "";
        }
        remoteAddr = remoteAddr.trim();

        if (!isLoopback(remoteAddr)) {
            return remoteAddr;
        }

        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded == null) {
            return remoteAddr;
        }

        // 뒤에서부터 — 빈 칸(", ,") 이 끼어도 실제 값을 집음
        String[] hops = forwarded.split(",");
        for (int i = hops.length - 1; i >= 0; i--) {
            String hop = hops[i].trim();
            if (!hop.isEmpty()) {
                return hop;
            }
        }
        return remoteAddr;
    }

    /** nginx 와 톰캣이 같은 장비 — 프록시를 거친 요청의 출처 */
    private static boolean isLoopback(String address) {
        return "127.0.0.1".equals(address)
                || "::1".equals(address)
                || "0:0:0:0:0:0:0:1".equals(address)
                || address.startsWith("127.");
    }

    /**
     * 컨텍스트 경로를 뗀 정규화 경로. 항상 "/" 로 시작.
     *
     * 원본 URI 로 인증 예외를 판정하면 /admin/x/../admin-schedule 같은 요청에 뚫린다.
     * 톰캣·스프링이 쓰는 경로와 모양이 같아야 함 — 어긋나면
     * 필터는 통과시키고 스프링은 관리자 화면으로 보내는 상태가 된다
     */
    public static String normalizedPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();

        String path = (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath))
                ? uri.substring(contextPath.length())
                : uri;

        // 디코딩보다 먼저 — 톰캣과 같은 순서.
        // 뒤에 떼면 %3B 로 보낸 진짜 세미콜론까지 잘려 반대로 어긋남
        path = stripPathParameters(path);

        try {
            path = URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException | IllegalArgumentException e) {
            // 디코딩 불가 시 원본 그대로 정규화
        }

        // 역슬래시를 구분자로 받는 환경 대비(윈도우)
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
     * 경로 파라미터 제거 — 세그먼트마다 ';' 부터 다음 '/' 까지.
     *
     * /admin;x=1/schedule 을 톰캣도 스프링도 /admin/schedule 로 읽는다.
     * 여기서만 ';x=1' 을 들고 있으면 "/admin/" 으로 안 보여 인증 검사를 건너뛴다
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
                // 마지막 세그먼트 — 뒤는 전부 파라미터
                return stripped.toString();
            }

            from = slash;
            semicolon = path.indexOf(';', slash);
        }

        return stripped.append(path.substring(from)).toString();
    }
}

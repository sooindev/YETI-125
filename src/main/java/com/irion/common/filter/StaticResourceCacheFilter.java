package com.irion.common.filter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 캐시 재검증 + 보안 헤더.
 *
 * 톰캣은 HTML/CSS/JS 에 ETag 만 붙이고 Cache-Control 을 보내지 않아, 브라우저가
 * 자체 휴리스틱으로 유효기간을 정한다. 그러면 배포 후에도 새 HTML 과 예전 CSS 가
 * 섞여 화면이 깨진다. no-cache 는 "캐시하지 말라"가 아니라 "쓰기 전에 확인하라"라,
 * 파일이 그대로면 304 만 오간다. 이미지는 제외 — 용량이 크고 파일명이 바뀐다.
 *
 * 보안 헤더는 정적 파일이든 API 든 같이 나가야 하므로 /* 매핑인 여기서 붙인다.
 */
public class StaticResourceCacheFilter implements Filter {

    /**
     * Content-Security-Policy
     *
     * script-src 에 'unsafe-inline' 이 없다 — 인라인 script 도 onclick 도 쓰지
     * 않는다. 되살리면 CSP 로 막으려던 것의 대부분이 무의미해지므로 인라인
     * 핸들러를 새로 넣지 말 것.
     *
     * style-src 는 'unsafe-inline' 을 남긴다. 일정 색상을 style 속성으로 주고,
     * FullCalendar 6 이 자기 CSS 를 JS 에서 심는다. 스타일 주입은 스크립트
     * 실행으로 이어지지 않는다.
     *
     * img-src 의 https: 는 치지직 썸네일이 여러 네이버 CDN 에서 오기 때문이다.
     */
    private static final String CSP = String.join("; ",
            "default-src 'self'",
            "script-src 'self' https://code.jquery.com https://cdn.jsdelivr.net",
            "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com",
            "img-src 'self' data: https:",
            "font-src 'self' data: https://fonts.gstatic.com",
            "connect-src 'self'",
            "frame-src https://chzzk.naver.com",
            "object-src 'none'",
            "base-uri 'self'",
            "form-action 'self'",
            "frame-ancestors 'none'");

    /**
     * HSTS. 평문 응답에 실리면 브라우저가 무시하므로 조건 없이 붙인다
     * (nginx 가 TLS 를 끊고 톰캣에는 평문으로 넘겨서 isSecure() 로는 판단 불가).
     *
     * includeSubDomains 는 뺐다 — HTTPS 가 아닌 서브도메인이 생기면 접속이 막힌다.
     */
    private static final String HSTS = "max-age=31536000";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String uri = httpRequest.getRequestURI();

        if (isPagePath(uri) || uri.endsWith(".html") || uri.endsWith(".css") || uri.endsWith(".js")) {
            httpResponse.setHeader("Cache-Control", "no-cache");
        }

        httpResponse.setHeader("Content-Security-Policy", CSP);
        httpResponse.setHeader("X-Frame-Options", "DENY");
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        httpResponse.setHeader("Strict-Transport-Security", HSTS);

        chain.doFilter(request, response);
    }

    /**
     * 페이지 주소는 확장자가 없다 — /schedule 처럼.
     *
     * 확장자로만 걸러내면 정규 주소가 된 페이지들이 Cache-Control 없이 나가
     * 톰캣 휴리스틱 캐시로 되돌아간다. 이 필터를 만든 이유가 바로 그것이다.
     * API 응답(/live/status 등)도 같이 걸리는데 매번 확인하는 편이 맞다.
     */
    private static boolean isPagePath(String uri) {
        return uri.indexOf('.', uri.lastIndexOf('/') + 1) < 0;
    }

    @Override
    public void destroy() {
    }
}

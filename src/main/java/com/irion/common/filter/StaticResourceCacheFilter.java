package com.irion.common.filter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 캐시 재검증 + 보안 헤더.
 *
 * Cache-Control 이 없으면 브라우저 휴리스틱 캐시 탓에 새 HTML 과 옛 CSS 가 섞인다.
 * no-cache 는 "쓰기 전에 확인하라"라 파일이 그대로면 304 만 오간다. 이미지는 제외.
 */
public class StaticResourceCacheFilter implements Filter {

    /**
     * script-src 에 'unsafe-inline' 이 없다 — 인라인 script/onclick 을 새로 넣지 말 것.
     * style-src 는 FullCalendar 가 CSS 를 JS 로 심어서, img-src 의 https: 는 치지직 썸네일 때문.
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

    /** nginx 가 TLS 를 끊어 isSecure() 로 못 가리므로 조건 없이 붙인다. includeSubDomains 는 제외. */
    private static final String HSTS = "max-age=31536000";

    /** 브라우저 기본값과 같지만 명시한다 — 기본값은 언제든 바뀔 수 있다. */
    private static final String REFERRER_POLICY = "strict-origin-when-cross-origin";

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
        httpResponse.setHeader("Referrer-Policy", REFERRER_POLICY);

        chain.doFilter(request, response);
    }

    /** 페이지 주소는 확장자가 없다(/schedule). 확장자로만 걸러내면 이 필터가 무의미해진다. */
    private static boolean isPagePath(String uri) {
        return uri.indexOf('.', uri.lastIndexOf('/') + 1) < 0;
    }

    @Override
    public void destroy() {
    }
}

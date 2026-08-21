package com.irion.common.filter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 캐시 재검증 + 보안 헤더 필터
 *
 * ── 캐시 재검증 (HTML, CSS, JS)
 *
 * 톰캣은 이 파일들에 ETag / Last-Modified 만 붙이고 Cache-Control 은 보내지
 * 않는다. 그러면 브라우저가 자체 휴리스틱으로 유효기간을 정해버려서,
 * 배포로 파일이 바뀌어도 한동안 서버에 묻지 않고 예전 것을 계속 쓴다.
 * 새 HTML 과 예전 CSS 가 섞여 화면이 깨지는 원인이 된다.
 *
 * no-cache 는 "캐시하지 말라"가 아니라 "쓰기 전에 서버에 확인하라"는 뜻이다.
 * 파일이 그대로면 304 만 오가므로 대역폭 부담은 거의 없다.
 *
 * 이미지는 제외했다. 파일명이 바뀌는 경우가 대부분이고 용량이 커서,
 * 매 요청 재검증까지 걸 이유가 없다.
 *
 * ── 보안 헤더 (모든 응답)
 *
 * 이 필터가 /* 에 매핑돼 있어 모든 응답을 지나간다. 정적 파일이든 API 든
 * 같은 헤더가 나가야 하므로 여기서 함께 붙인다.
 */
public class StaticResourceCacheFilter implements Filter {

    /**
     * Content-Security-Policy
     *
     * script-src 에 'unsafe-inline' 이 없다. 페이지에 인라인 &lt;script&gt; 도
     * onclick 속성도 남기지 않았기 때문이다 (닫기 버튼은 data-close-modal
     * 위임 핸들러로 바꿨다). 여기에 'unsafe-inline' 을 되살리면 CSP 로
     * 막으려던 것의 대부분이 무의미해지므로, 인라인 핸들러를 새로 넣지 말 것.
     *
     * style-src 는 'unsafe-inline' 을 남긴다. 일정 색상처럼 style 속성을
     * 직접 쓰는 자리가 있고, FullCalendar 6 은 자기 CSS 를 JS 에서 &lt;style&gt; 로
     * 심는다. 스타일 주입은 스크립트 실행으로 이어지지 않는다.
     *
     * 모든 페이지가 Google Fonts 를 쓴다. 스타일시트는 fonts.googleapis.com
     * 에서, 실제 폰트 파일은 fonts.gstatic.com 에서 온다. 둘 다 열어야
     * 글꼴이 깨지지 않는다.
     *
     * img-src 에 https: 를 열어둔 것은 치지직 썸네일이 여러 네이버 CDN
     * 호스트에서 오기 때문이다. frame-src 는 클립 임베드용 한 곳만 연다.
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
     * HSTS
     *
     * 평문 HTTP 응답에 실리면 브라우저가 무시하므로 조건 없이 붙인다.
     * (운영은 nginx 가 HTTPS 를 끊고 톰캣에는 평문으로 넘기기 때문에
     *  request.isSecure() 로 판단하면 영영 안 나간다)
     *
     * includeSubDomains 는 넣지 않았다. HTTPS 가 아닌 서브도메인이 하나라도
     * 있으면 그때부터 접속이 막힌다. 서브도메인 상황이 확실해지면 추가할 것.
     */
    private static final String HSTS = "max-age=31536000";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 초기화 작업 없음
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String uri = httpRequest.getRequestURI();

        if (uri.endsWith(".html") || uri.endsWith(".css") || uri.endsWith(".js")) {
            httpResponse.setHeader("Cache-Control", "no-cache");
        }

        httpResponse.setHeader("Content-Security-Policy", CSP);
        httpResponse.setHeader("X-Frame-Options", "DENY");
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        httpResponse.setHeader("Strict-Transport-Security", HSTS);

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // 정리 작업 없음
    }
}

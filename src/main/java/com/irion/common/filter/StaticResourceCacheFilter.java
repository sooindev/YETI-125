package com.irion.common.filter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 정적 리소스(HTML, CSS, JS) 캐시 재검증 필터
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
 */
public class StaticResourceCacheFilter implements Filter {

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

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // 정리 작업 없음
    }
}

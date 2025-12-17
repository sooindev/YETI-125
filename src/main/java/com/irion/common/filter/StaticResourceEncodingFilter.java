package com.irion.common.filter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 정적 리소스(JS, CSS, HTML)에 대한 UTF-8 인코딩 필터
 * 이모티콘 등 비-ASCII 문자가 제대로 표시되도록 Content-Type에 charset 추가
 */
public class StaticResourceEncodingFilter implements Filter {

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

        // 정적 리소스에 대해 Content-Type charset 설정
        if (uri.endsWith(".js")) {
            httpResponse.setContentType("application/javascript; charset=UTF-8");
        } else if (uri.endsWith(".css")) {
            httpResponse.setContentType("text/css; charset=UTF-8");
        } else if (uri.endsWith(".html")) {
            httpResponse.setContentType("text/html; charset=UTF-8");
        } else if (uri.endsWith(".json")) {
            httpResponse.setContentType("application/json; charset=UTF-8");
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // 정리 작업 없음
    }
}

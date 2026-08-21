package com.irion.common.filter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 정적 리소스(JS, CSS)에 대한 UTF-8 인코딩 필터
 *
 * 이모티콘 등 비-ASCII 문자가 제대로 표시되도록 Content-Type 에 charset 을 붙인다.
 *
 * web.xml 에서 /resources/* 에만 매핑돼 있다. 그 아래에는 css / js 와
 * 이미지뿐이라, 예전에 있던 .html / .json 분기는 한 번도 실행되지 않는
 * 죽은 코드였다. (HTML 은 /resources 밖에 있다)
 *
 * 매핑을 넓히게 되면 그때 분기를 다시 넣을 것.
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

        if (uri.endsWith(".js")) {
            httpResponse.setContentType("application/javascript; charset=UTF-8");
        } else if (uri.endsWith(".css")) {
            httpResponse.setContentType("text/css; charset=UTF-8");
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // 정리 작업 없음
    }
}

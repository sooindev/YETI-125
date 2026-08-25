package com.irion.common.filter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/** 정적 JS/CSS 의 Content-Type 에 charset 을 붙인다. /resources/* 에는 css/js/이미지뿐이다. */
public class StaticResourceEncodingFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
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
    }
}

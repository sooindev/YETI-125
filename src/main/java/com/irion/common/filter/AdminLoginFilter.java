package com.irion.common.filter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

public class AdminLoginFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String uri = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();

        // /admin/** 경로 체크 (login 제외)
        if (uri.startsWith(contextPath + "/admin/") &&
                !uri.contains("/admin/login") &&
                !uri.contains("/admin/admin-login") &&
                !uri.contains("/admin/loginProc")) {

            HttpSession session = httpRequest.getSession(false);

            if (session == null || session.getAttribute("adminUser") == null) {
                httpResponse.sendRedirect(contextPath + "/admin/admin-login.html");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}
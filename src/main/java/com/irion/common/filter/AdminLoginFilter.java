package com.irion.common.filter;

import com.irion.common.util.RequestUtil;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class AdminLoginFilter implements Filter {

    /** 로그인 없이 지나갈 경로. contains 로 판정하면 /admin/loginProc/../ 같은 요청에 뚫린다. */
    private static final Set<String> PUBLIC_PATHS = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList(
                    "/admin/admin-login",
                    "/admin/loginProc")));

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String contextPath = httpRequest.getContextPath();
        String path = RequestUtil.normalizedPath(httpRequest);

        if (isAdminPath(path) && !PUBLIC_PATHS.contains(path)) {

            HttpSession session = httpRequest.getSession(false);

            if (session == null || session.getAttribute("adminUser") == null) {
                // AJAX 에 302 를 주면 jQuery 가 따라가 로그인 HTML 을 200 으로 받는다
                if (RequestUtil.isAjaxRequest(httpRequest)) {
                    httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    httpResponse.setContentType("application/json;charset=UTF-8");
                    httpResponse.getWriter().write("{\"success\":false,\"message\":\"로그인이 필요합니다.\"}");
                    return;
                }

                httpResponse.sendRedirect(contextPath + "/admin/admin-login");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    /**
     * 관리자 영역인가. "/admin/" 으로만 보면 정확히 "/admin" 인 요청이 빠져나간다 —
     * 톰캣의 /admin/* 매핑과 스프링의 /admin/** 매핑은 둘 다 그것까지 관리자로 본다.
     */
    private static boolean isAdminPath(String path) {
        return "/admin".equals(path) || path.startsWith("/admin/");
    }

    @Override
    public void destroy() {
    }
}

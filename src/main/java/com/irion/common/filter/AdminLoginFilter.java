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

    /**
     * 로그인 없이 지나갈 수 있는 경로.
     *
     * 부분 문자열이 아니라 정확히 일치하는지 본다. contains 로 판정하면
     * /admin/loginProc/../admin-schedule.html 같은 요청에 뚫린다.
     */
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

        if (path.startsWith("/admin/") && !PUBLIC_PATHS.contains(path)) {

            HttpSession session = httpRequest.getSession(false);

            if (session == null || session.getAttribute("adminUser") == null) {
                /*
                 * AJAX 에 리다이렉트를 주면 안 된다. jQuery 가 302 를 따라가
                 * 로그인 HTML 을 200 으로 받아버려서, 화면은 세션이 끊긴 것을
                 * 알아채지 못하고 조용히 빈 채로 남는다.
                 */
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

    @Override
    public void destroy() {
    }
}

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
     * 로그인 없이 지나갈 수 있는 관리자 경로.
     *
     * contains 가 아니라 정확히 일치하는지 본다. 예전에는
     * uri.contains("/admin/login") 으로 걸렀는데, getRequestURI() 는
     * 정규화 전 원본이라 /admin/login/../admin-schedule.html 같은 요청이
     * "포함하니까 공개" 로 통과해 버렸다.
     */
    private static final Set<String> PUBLIC_PATHS = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList(
                    "/admin/admin-login",
                    "/admin/admin-login.html",
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
                 * 이 필터는 DispatcherServlet 앞에서 돈다. 여기서 무조건
                 * 리다이렉트해 버리면 AdminLoginInterceptor 의 401 분기까지
                 * 요청이 닿지 못한다. jQuery 는 302 를 따라가 로그인 HTML 을
                 * 200 으로 받고, JSON 파싱에 실패해 error 콜백으로 가는데
                 * 그 때 xhr.status 는 200 이라 로그인 화면으로 보낼 수 없다.
                 * 세션이 끊기면 관리자 화면이 조용히 비었다.
                 */
                if (RequestUtil.isAjaxRequest(httpRequest)) {
                    httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    httpResponse.setContentType("application/json;charset=UTF-8");
                    httpResponse.getWriter().write("{\"success\":false,\"message\":\"로그인이 필요합니다.\"}");
                    return;
                }

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

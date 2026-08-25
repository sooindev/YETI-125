package com.irion.common.filter;

import com.irion.common.util.CsrfTokens;
import com.irion.common.util.RequestUtil;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** 관리자 CSRF 방어. 상태를 바꾸는 메서드만 검사하고, 세션 이전인 로그인은 예외다. */
public class CsrfFilter implements Filter {

    private static final Set<String> PROTECTED_METHODS = new HashSet<String>(
            Arrays.asList("POST", "PUT", "DELETE", "PATCH"));

    /** 세션이 없는 시점의 요청 */
    private static final Set<String> EXEMPT_PATHS = new HashSet<String>(
            Arrays.asList("/admin/loginProc"));

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = RequestUtil.normalizedPath(httpRequest);

        if (PROTECTED_METHODS.contains(httpRequest.getMethod())
                && !EXEMPT_PATHS.contains(path)
                && !CsrfTokens.isValid(httpRequest)) {

            httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
            if (RequestUtil.isAjaxRequest(httpRequest)) {
                httpResponse.setContentType("application/json;charset=UTF-8");
                httpResponse.getWriter().write(
                        "{\"success\":false,\"message\":\"요청이 만료되었습니다. 새로고침 후 다시 시도해 주세요.\"}");
            } else {
                httpResponse.setContentType("text/plain;charset=UTF-8");
                httpResponse.getWriter().write("CSRF token mismatch");
            }
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}

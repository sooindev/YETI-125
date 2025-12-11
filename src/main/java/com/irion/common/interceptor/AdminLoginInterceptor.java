package com.irion.common.interceptor;

import com.irion.admin.vo.AdminVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class AdminLoginInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AdminLoginInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        HttpSession session = request.getSession(false);

        // 로그인 안 된 경우
        if (session == null || session.getAttribute("adminUser") == null) {
            logger.debug("Admin not logged in, redirecting to login page");

            // AJAX 요청인 경우
            if (isAjaxRequest(request)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"success\":false,\"message\":\"로그인이 필요합니다.\"}");
                return false;
            }

            // 일반 요청인 경우
            response.sendRedirect(request.getContextPath() + "/admin/admin-login.html");
            return false;
        }

        AdminVO admin = (AdminVO) session.getAttribute("adminUser");
        logger.debug("Admin logged in: {}", admin.getAdminLoginId());

        return true;
    }

    private boolean isAjaxRequest(HttpServletRequest request) {
        String ajaxHeader = request.getHeader("X-Requested-With");
        return "XMLHttpRequest".equals(ajaxHeader) ||
                request.getContentType() != null && request.getContentType().contains("application/json");
    }

}
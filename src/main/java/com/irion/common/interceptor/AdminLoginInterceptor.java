package com.irion.common.interceptor;

import com.irion.admin.vo.AdminVO;
import com.irion.common.util.RequestUtil;
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

        if (session == null || session.getAttribute("adminUser") == null) {
            logger.debug("Admin not logged in, redirecting to login page");

            if (RequestUtil.isAjaxRequest(request)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"success\":false,\"message\":\"로그인이 필요합니다.\"}");
                return false;
            }

            response.sendRedirect(request.getContextPath() + "/admin/admin-login");
            return false;
        }

        AdminVO admin = (AdminVO) session.getAttribute("adminUser");
        logger.debug("Admin logged in: {}", admin.getAdminLoginId());

        return true;
    }

}
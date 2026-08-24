package com.irion.common.filter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 예전 .html 주소를 확장자 없는 정규 주소로 301 한다.
 *
 * 같은 내용이 두 주소로 200 을 주면 검색엔진이 어느 쪽이 진짜인지 스스로 정하고,
 * 밖에서 걸어둔 링크도 두 갈래로 갈린다. 한쪽만 남기고 나머지는 넘긴다.
 *
 * REQUEST 디스패치에만 걸린다(필터 기본값). 컨트롤러가 /schedule 을
 * /schedule.html 로 forward 해서 실제 파일을 꺼내오는데, 그 forward 까지
 * 여기로 들어오면 301 → forward → 301 로 끝없이 돈다.
 */
public class LegacyHtmlRedirectFilter implements Filter {

    /** 옮긴 주소. 값이 정규 주소다 */
    private static final Map<String, String> MOVED;

    static {
        Map<String, String> moved = new HashMap<>();
        moved.put("/index.html", "/");
        moved.put("/info.html", "/info");
        moved.put("/schedule.html", "/schedule");
        moved.put("/admin/admin-login.html", "/admin/admin-login");
        moved.put("/admin/admin-schedule.html", "/admin/schedule");
        MOVED = Collections.unmodifiableMap(moved);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 루트 배포가 아닐 수도 있어 컨텍스트 경로를 떼고 본다
        String contextPath = httpRequest.getContextPath();
        String path = httpRequest.getRequestURI().substring(contextPath.length());

        String target = MOVED.get(path);
        if (target == null) {
            chain.doFilter(request, response);
            return;
        }

        String query = httpRequest.getQueryString();
        String location = contextPath + target + (query != null ? "?" + query : "");

        // sendRedirect 는 302 다. 영구 이전이므로 301 을 직접 쓴다
        httpResponse.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
        httpResponse.setHeader("Location", location);
    }

    @Override
    public void destroy() {
    }
}

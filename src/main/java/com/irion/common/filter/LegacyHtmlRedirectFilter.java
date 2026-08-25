package com.irion.common.filter;

import com.irion.common.util.RequestUtil;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 주소를 정규 주소 하나로 모은다 — 옛 .html, 그리고 //schedule 이나 /schedule/ 같은 다른 표기.
 *
 * REQUEST 디스패치에만 걸린다(필터 기본값). forward 까지 걸리면 301 과 forward 가 서로를 부른다.
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

        // 루트 배포가 아닐 수 있어 컨텍스트 경로를 떼고 본다
        String contextPath = httpRequest.getContextPath();
        String path = httpRequest.getRequestURI().substring(contextPath.length());

        String target = canonicalOf(httpRequest, path);
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

    /** 보내야 할 정규 주소. 이미 정규 주소면 null */
    private static String canonicalOf(HttpServletRequest request, String path) {

        // %2E 나 // 같은 표기로도 옛 주소에 닿으므로 정규화해서 판정한다.
        // 목적지는 표에 박힌 값 — 요청에서 만들면 되돌아온 요청이 또 걸린다.
        String moved = MOVED.get(RequestUtil.normalizedPath(request));
        if (moved != null) {
            return moved;
        }

        // 표기만 손본다. 디코딩하면 브라우저가 다시 인코딩해 보내 무한 왕복이 된다.
        String tidied = collapseSlashes(path);
        return tidied.equals(path) ? null : tidied;
    }

    /** 겹친 슬래시를 하나로, 끝 슬래시는 뗀다. 루트(/)는 그대로 둔다 */
    private static String collapseSlashes(String path) {
        StringBuilder out = new StringBuilder(path.length());
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == '/' && out.length() > 0 && out.charAt(out.length() - 1) == '/') {
                continue;
            }
            out.append(c);
        }
        if (out.length() > 1 && out.charAt(out.length() - 1) == '/') {
            out.setLength(out.length() - 1);
        }
        return out.toString();
    }

    @Override
    public void destroy() {
    }
}

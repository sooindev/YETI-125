package com.irion.common.web.filter;

import com.irion.common.web.RequestUtil;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 주소를 정규 주소로 통일 — 옛 .html, //schedule, /schedule/ 같은 표기.
 * REQUEST 디스패치에만(필터 기본값) — forward 까지 걸리면 301 과 forward 가 서로를 부른다
 */
public class LegacyHtmlRedirectFilter implements Filter {

    /** 이전된 주소 표. 값이 정규 주소 */
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

        // 루트 배포가 아닐 수 있어 컨텍스트 경로 제거 후 판정
        String contextPath = httpRequest.getContextPath();
        String path = httpRequest.getRequestURI().substring(contextPath.length());

        String target = canonicalOf(httpRequest, path);
        if (target == null) {
            chain.doFilter(request, response);
            return;
        }

        String query = httpRequest.getQueryString();
        String location = contextPath + target + (query != null ? "?" + query : "");

        // sendRedirect 는 302 — 영구 이전이라 301 을 직접
        httpResponse.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
        httpResponse.setHeader("Location", location);
    }

    /** 보낼 정규 주소. 이미 정규면 null */
    private static String canonicalOf(HttpServletRequest request, String path) {

        // %2E 나 // 표기로도 옛 주소에 닿으므로 정규화 후 판정.
        // 목적지는 표에 박힌 값 — 요청에서 만들면 되돌아온 요청이 또 걸린다
        String moved = MOVED.get(RequestUtil.normalizedPath(request));
        if (moved != null) {
            return moved;
        }

        // 표기만 수정. 디코딩하면 브라우저가 재인코딩해 무한 왕복
        String tidied = collapseSlashes(path);
        return tidied.equals(path) ? null : tidied;
    }

    /** 중복 슬래시 병합 + 끝 슬래시 제거. 루트(/)는 유지 */
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

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
 * 주소를 하나로 모은다.
 *
 *   1. 예전 .html 주소  → 확장자 없는 정규 주소
 *   2. 같은 곳을 가리키는 다른 표기 → 정규 표기
 *      (//schedule, /schedule/, /schedule// …)
 *
 * 같은 내용이 여러 주소로 200 을 주면 검색엔진이 어느 쪽이 진짜인지 스스로
 * 정하고, 밖에서 걸어둔 링크도 갈래갈래 갈린다.
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

        /*
         * 옮긴 주소인지는 정규화한 경로로 판정한다.
         *
         * /schedule%2Ehtml 이나 //schedule.html 처럼 글자만 다른 표기로도
         * 옛 페이지에 닿을 수 있다. 목적지는 아래 표에 박아둔 값이라
         * 요청에서 만들어내지 않는다 — 그래야 되돌아오는 요청이 다시
         * 걸리는 일이 없다.
         */
        String moved = MOVED.get(RequestUtil.normalizedPath(request));
        if (moved != null) {
            return moved;
        }

        /*
         * 그 외에는 표기만 손본다. 여기서는 퍼센트 디코딩을 하지 않는다.
         * 디코딩한 주소로 보내면 브라우저가 다시 인코딩해서 돌아오고,
         * 그것을 또 디코딩해 보내는 무한 왕복이 된다.
         */
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

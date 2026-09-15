package com.irion.common.web.filter;

import com.irion.common.web.RequestUtil;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 내려둔 화면의 입구 차단 — 3주년이 끝난 방명록.
 *
 * 컨트롤러 · SQL · 테이블은 그대로 둔다. 되살릴 때는 web.xml 의 이 필터 매핑만 떼면 된다.
 *
 * 컨트롤러에 스위치를 두지 않은 이유 — 화면 · 목록 · 글쓰기가 한 번에 닫혀야 하고
 * (링크만 떼면 주소를 아는 사람은 계속 쓴다), 업무 규칙 테스트도 살아 있어야 한다
 */
public class ClosedPageFilter implements Filter {

    /** 차단 구역. 이 경로와 하위 전부 */
    private static final Set<String> CLOSED = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList("/guestbook")));

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (!isClosed(httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        // 주소창 진입은 빈 화면 대신 홈으로.
        // 302 인 이유 — 301 은 브라우저가 영구 캐시해 되살린 뒤에도 홈으로 튄다
        String method = httpRequest.getMethod();
        if ("GET".equals(method) || "HEAD".equals(method)) {
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/");
            return;
        }

        // 글쓰기 호출은 홈으로 보낼 것이 없다 — 없는 주소로
        httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    /**
     * 차단 구역 여부. 원본 주소로 판정하면 /guestbook%2Flist 같은 표기로 비켜 가므로
     * 톰캣·스프링과 같은 모양으로 정규화한 뒤 본다
     */
    private static boolean isClosed(HttpServletRequest request) {
        String path = RequestUtil.normalizedPath(request);

        for (String closed : CLOSED) {
            if (path.equals(closed) || path.startsWith(closed + "/")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void destroy() {
    }
}

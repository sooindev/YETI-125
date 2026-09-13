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
 * 내려둔 공개 화면의 입구를 닫는다 — 3주년이 끝나 더는 보이지 않는 방명록이 그것이다.
 *
 * 컨트롤러 · SQL · 테이블은 손대지 않는다. 글은 DB 에 그대로 있고, 되살릴 때는
 * web.xml 의 이 필터 매핑만 떼면 화면과 API 가 그대로 돌아온다.
 *
 * 컨트롤러에 스위치를 두지 않은 이유는 두 가지다. 하나는 화면 · 목록 · 글쓰기가
 * 한 번에 닫혀야 한다는 것 — 메뉴에서만 링크를 떼면 주소를 아는 사람은 계속 쓴다.
 * 다른 하나는 컨트롤러의 업무 규칙(익명 고정 · 간격 제한)을 검증하는 테스트가
 * 그대로 살아 있어야 되살릴 때 믿고 열 수 있다는 것이다.
 */
public class ClosedPageFilter implements Filter {

    /** 내려둔 구역. 이 경로와 그 아래가 전부 막힌다 */
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

        // 주소창으로 들어온 사람은 빈 화면 대신 홈으로 보낸다.
        //
        // 302 다 — 301 로 보내면 브라우저가 영구 캐시해, 되살린 뒤에도 한동안
        // 홈으로 튄다. 한시적으로 내려둔 화면에 쓸 상태 코드가 아니다.
        String method = httpRequest.getMethod();
        if ("GET".equals(method) || "HEAD".equals(method)) {
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/");
            return;
        }

        // 글쓰기 같은 호출은 홈으로 보낼 것이 없다. 화면이 사라졌으니 없는 주소다
        httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    /**
     * 닫아 둔 구역인가.
     *
     * 원본 주소로 판정하면 /guestbook%2Flist 나 /guestbook;x=1 같은 표기로 비켜 간다 —
     * 톰캣과 스프링이 보는 경로와 같은 모양으로 맞춘 뒤에 본다.
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

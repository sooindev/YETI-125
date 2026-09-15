package com.irion.common.web.filter;

import com.irion.common.security.LoginRateLimiter;
import com.irion.common.web.RequestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 로그인 요청 빈도 제한 — 주소 기준.
 *
 * /admin/loginProc 은 CSRF 면제인데 비밀번호 해시는 일부러 느리다(PBKDF2 210,000회 ≈ 280ms).
 * 그 느림이 공격자에게 넘어가 요청 한 건이 서버에서 280ms 의 계산이 된다.
 * LoginAttemptGuard 는 계정 기준이라 아이디를 매번 바꾸면 안 걸린다 — 그 구멍을 여기서 막는다.
 *
 * 컨트롤러가 아니라 필터인 이유 — 해시 계산 앞에서 끊어야 한다.
 * 1차 방어는 nginx 의 limit_req(config/nginx), 이 필터는 그물이라 한도를 더 느슨하게 둔다
 */
public class LoginRateLimitFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(LoginRateLimitFilter.class);

    /** javax.servlet 에 429 상수 없음 */
    private static final int SC_TOO_MANY_REQUESTS = 429;

    private static final String LOGIN_PATH = "/admin/loginProc";

    /** 매번 남기면 공격이 곧 로그 폭탄 */
    private static final long LOG_INTERVAL_MILLIS = 60 * 1000L;

    private final LoginRateLimiter limiter = new LoginRateLimiter();
    private final AtomicLong lastLoggedAt = new AtomicLong();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 원본 주소로 판정하면 /admin/x/../loginProc 이 비켜 감
        if (!"POST".equals(httpRequest.getMethod())
                || !LOGIN_PATH.equals(RequestUtil.normalizedPath(httpRequest))) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = RequestUtil.clientIp(httpRequest);
        long retryAfter = limiter.retryAfterSeconds(clientIp);

        if (retryAfter > 0) {
            logThrottled(clientIp, retryAfter);
            reject(httpRequest, httpResponse, retryAfter);
            return;
        }

        chain.doFilter(request, response);
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, long retryAfter)
            throws IOException {

        response.setStatus(SC_TOO_MANY_REQUESTS);
        response.setHeader("Retry-After", String.valueOf(retryAfter));

        String message = "로그인 시도가 너무 많습니다. " + retryAfter + "초 후 다시 시도해 주세요.";

        // 화면이 JSON 을 기대한다. HTML 을 주면 "오류" 로 뭉개짐
        if (RequestUtil.isAjaxRequest(request)) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"" + message + "\"}");
        } else {
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write(message);
        }
    }

    /** 한 번 막히면 계속 막힌다. 창마다 한 줄이면 경위는 남는다 */
    private void logThrottled(String clientIp, long retryAfter) {
        long now = System.currentTimeMillis();
        long last = lastLoggedAt.get();

        if (now - last < LOG_INTERVAL_MILLIS || !lastLoggedAt.compareAndSet(last, now)) {
            return;
        }

        logger.warn("Login rate limited: {} ({}초 대기) — 같은 로그는 {}초간 생략합니다",
                mask(clientIp), retryAfter, LOG_INTERVAL_MILLIS / 1000);
    }

    /** 주소 일부만 기록 — 계정 이름을 가리는 것과 같은 이유 */
    static String mask(String ip) {
        if (ip == null || ip.isEmpty()) {
            return "(unknown)";
        }

        int lastDot = ip.lastIndexOf('.');
        if (lastDot > 0) {
            return ip.substring(0, lastDot) + ".x";
        }

        // IPv6 — 앞 두 덩이만
        int second = ip.indexOf(':', ip.indexOf(':') + 1);
        return second > 0 ? ip.substring(0, second) + ":x" : "x";
    }

    @Override
    public void destroy() {
    }
}

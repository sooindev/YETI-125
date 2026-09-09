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
 * /admin/loginProc 은 세션 이전이라 CSRF 검사가 면제고, 비밀번호 해시는 일부러 느리다
 * (PBKDF2 210,000회 ≈ 280ms). 그 느림이 공격자에게 그대로 넘어가 <b>요청 한 건이 서버에서
 * 280ms 의 계산</b>이 된다. LoginAttemptGuard 는 계정 기준이라 매번 다른 아이디를 보내면
 * 5회 한도에 영원히 닿지 않고, 없는 아이디에도 계정 열거를 막으려 같은 계산을 태운다.
 * 그 구멍을 여기서 주소 기준으로 막는다.
 *
 * 컨트롤러가 아니라 필터인 이유는 <b>해시 계산 앞에서</b> 끊기 위해서다. 컨트롤러까지
 * 들어가면 이미 늦다.
 *
 * 1차 방어는 앞단 nginx 의 limit_req 다 (config/nginx). 요청이 톰캣 스레드를 잡기 전에
 * 잘라내므로 그쪽이 더 싸다. 이 필터는 nginx 설정이 빠졌거나 서버를 새로 세웠을 때를 위한
 * 그물이라, 한도를 nginx 보다 느슨하게 둔다.
 */
public class LoginRateLimitFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(LoginRateLimitFilter.class);

    /** javax.servlet 에는 429 상수가 없다 */
    private static final int SC_TOO_MANY_REQUESTS = 429;

    private static final String LOGIN_PATH = "/admin/loginProc";

    /** 막힌 요청마다 한 줄씩 남기면 공격이 곧 로그 폭탄이 된다 */
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

        // 원본 주소로 판정하면 /admin/x/../loginProc 이 이 검사를 비켜 간다
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

        // 화면은 jQuery 로 보내므로 JSON 을 기대한다. HTML 을 주면 그냥 "오류" 로 뭉개진다
        if (RequestUtil.isAjaxRequest(request)) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"" + message + "\"}");
        } else {
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write(message);
        }
    }

    /** 한 번 막히기 시작하면 계속 막힌다. 창마다 한 줄이면 무슨 일이 있었는지는 남는다. */
    private void logThrottled(String clientIp, long retryAfter) {
        long now = System.currentTimeMillis();
        long last = lastLoggedAt.get();

        if (now - last < LOG_INTERVAL_MILLIS || !lastLoggedAt.compareAndSet(last, now)) {
            return;
        }

        logger.warn("Login rate limited: {} ({}초 대기) — 같은 로그는 {}초간 생략합니다",
                mask(clientIp), retryAfter, LOG_INTERVAL_MILLIS / 1000);
    }

    /**
     * 주소를 통째로 남기지 않는다 — 계정 이름을 가리는 것과 같은 이유다.
     * 어디서 오는지 가늠할 만큼만 남긴다.
     */
    static String mask(String ip) {
        if (ip == null || ip.isEmpty()) {
            return "(unknown)";
        }

        int lastDot = ip.lastIndexOf('.');
        if (lastDot > 0) {
            return ip.substring(0, lastDot) + ".x";
        }

        // IPv6 — 앞 두 덩이만 남긴다
        int second = ip.indexOf(':', ip.indexOf(':') + 1);
        return second > 0 ? ip.substring(0, second) + ":x" : "x";
    }

    @Override
    public void destroy() {
    }
}

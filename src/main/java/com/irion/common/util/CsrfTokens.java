package com.irion.common.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * CSRF 토큰 발급/검증.
 *
 * 지금까지는 관리자 API 가 contentType: application/json 을 쓴다는 점이
 * 우연히 방패 노릇을 하고 있었다. 폼이나 &lt;img&gt; 로는 그 Content-Type 을
 * 만들 수 없기 때문이다. 하지만 그건 CSRF 방어가 아니라 부작용이라,
 * 요청 하나만 단순 폼으로 바뀌어도 그대로 뚫린다.
 *
 * 토큰은 로그인할 때 세션에 심고, 화면은 /admin/csrf-token 으로 받아
 * 상태를 바꾸는 요청마다 X-CSRF-Token 헤더에 실어 보낸다. 응답 본문은
 * 동일 출처 정책 때문에 다른 사이트에서 읽을 수 없다.
 */
public final class CsrfTokens {

    public static final String SESSION_KEY = "csrfToken";
    public static final String HEADER = "X-CSRF-Token";

    private static final SecureRandom RANDOM = new SecureRandom();

    private CsrfTokens() {
    }

    /** 세션의 토큰을 돌려준다. 없으면 새로 만들어 심는다. */
    public static String issue(HttpSession session) {
        Object existing = session.getAttribute(SESSION_KEY);
        if (existing instanceof String && !((String) existing).isEmpty()) {
            return (String) existing;
        }

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        session.setAttribute(SESSION_KEY, token);
        return token;
    }

    /** 로그인 직후처럼 세션을 새로 열 때 강제로 다시 발급한다. */
    public static String reissue(HttpSession session) {
        session.removeAttribute(SESSION_KEY);
        return issue(session);
    }

    /**
     * 요청에 실린 토큰이 세션의 것과 같은가.
     *
     * 세션이 없거나 토큰이 없으면 통과시키지 않는다. 비교는 길이로
     * 정답을 흘리지 않도록 MessageDigest.isEqual 로 한다.
     */
    public static boolean isValid(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }

        Object expected = session.getAttribute(SESSION_KEY);
        if (!(expected instanceof String)) {
            return false;
        }

        String presented = request.getHeader(HEADER);
        if (presented == null || presented.isEmpty()) {
            presented = request.getParameter("_csrf");
        }
        if (presented == null || presented.isEmpty()) {
            return false;
        }

        return MessageDigest.isEqual(
                ((String) expected).getBytes(StandardCharsets.UTF_8),
                presented.getBytes(StandardCharsets.UTF_8));
    }
}

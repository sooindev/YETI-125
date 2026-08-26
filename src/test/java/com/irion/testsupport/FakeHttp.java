package com.irion.testsupport;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * 서블릿 가짜 객체. 목 프레임워크 대신 JDK 동적 Proxy 를 쓴다.
 *
 * 필터와 인터셉터 테스트가 함께 쓴다 — 한쪽 패키지에 두면 다른 쪽이 못 가져다 쓴다.
 */
public final class FakeHttp {

    private FakeHttp() {
    }


    public static final class Request {
        private String uri = "/";
        private String contextPath = "";
        private String method = "GET";
        private HttpSession session;
        private String contentType;
        private String queryString;
        private final Map<String, String> headers = new HashMap<String, String>();
        private final Map<String, String> params = new HashMap<String, String>();

        /** getSession(true) 가 불렸는지 — 필터가 세션을 새로 만들면 안 된다 */
        public boolean sessionCreated;

        public Request uri(String value) {
            this.uri = value;
            return this;
        }

        public Request contextPath(String value) {
            this.contextPath = value;
            return this;
        }

        public Request method(String value) {
            this.method = value;
            return this;
        }

        public Request queryString(String value) {
            this.queryString = value;
            return this;
        }

        public Request session(HttpSession value) {
            this.session = value;
            return this;
        }

        public Request header(String name, String value) {
            this.headers.put(name, value);
            return this;
        }

        public Request contentType(String value) {
            this.contentType = value;
            return this;
        }

        public Request param(String name, String value) {
            this.params.put(name, value);
            return this;
        }

        /** 화면이 보내는 AJAX 요청 모양 */
        public Request ajax() {
            return header("X-Requested-With", "XMLHttpRequest").header("Accept", "application/json");
        }

        /** 주소창으로 들어온 요청 모양 */
        public Request browser() {
            return header("Accept", "text/html,application/xhtml+xml");
        }

        public HttpServletRequest build() {
            return (HttpServletRequest) Proxy.newProxyInstance(
                    HttpServletRequest.class.getClassLoader(),
                    new Class<?>[] { HttpServletRequest.class },
                    (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "getRequestURI":
                                return uri;
                            case "getContextPath":
                                return contextPath;
                            case "getMethod":
                                return this.method;
                            case "getContentType":
                                return contentType;
                            case "getQueryString":
                                return queryString;
                            case "getHeader":
                                return headers.get((String) args[0]);
                            case "getParameter":
                                return params.get((String) args[0]);
                            case "getSession":
                                boolean create = args == null || args.length == 0
                                        || Boolean.TRUE.equals(args[0]);
                                if (create && session == null) {
                                    sessionCreated = true;
                                    session = FakeHttp.session();  // 빌더 메서드에 가려짐
                                }
                                return session;
                            default:
                                return blank(method.getReturnType());
                        }
                    });
        }
    }


    /** 로그인 전 — 속성이 비어 있는 세션 */
    public static HttpSession session() {
        return session(new HashMap<String, Object>());
    }

    /** 관리자로 로그인된 세션 */
    public static HttpSession loggedIn() {
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("adminUser", "관리자");
        return session(attributes);
    }

    public static HttpSession session(Map<String, Object> attributes) {
        return (HttpSession) Proxy.newProxyInstance(
                HttpSession.class.getClassLoader(),
                new Class<?>[] { HttpSession.class },
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getAttribute":
                            return attributes.get((String) args[0]);
                        case "setAttribute":
                            attributes.put((String) args[0], args[1]);
                            return null;
                        case "removeAttribute":
                            attributes.remove((String) args[0]);
                            return null;
                        default:
                            return blank(method.getReturnType());
                    }
                });
    }


    public static final class Response {
        public int status = 200;
        public String redirect;
        public String contentType;
        private final Map<String, String> headers = new HashMap<String, String>();
        private final StringWriter written = new StringWriter();

        public String body() {
            return written.toString();
        }

        /** 301 은 sendRedirect 가 아니라 setStatus + Location 으로 나간다 */
        public String header(String name) {
            return headers.get(name);
        }

        public HttpServletResponse build() {
            return (HttpServletResponse) Proxy.newProxyInstance(
                    HttpServletResponse.class.getClassLoader(),
                    new Class<?>[] { HttpServletResponse.class },
                    (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "setStatus":
                                status = (Integer) args[0];
                                return null;
                            case "getStatus":
                                return status;
                            case "sendRedirect":
                                redirect = (String) args[0];
                                status = 302;
                                return null;
                            case "setHeader":
                                headers.put((String) args[0], (String) args[1]);
                                return null;
                            case "getHeader":
                                return headers.get((String) args[0]);
                            case "setContentType":
                                contentType = (String) args[0];
                                return null;
                            case "getContentType":
                                return contentType;
                            case "getWriter":
                                return new PrintWriter(written, true);
                            default:
                                return blank(method.getReturnType());
                        }
                    });
        }
    }


    public static final class Chain {
        /** 필터가 요청을 다음으로 넘겼는가 */
        public boolean passed;

        public FilterChain build() {
            return (request, response) -> passed = true;
        }
    }

    /** 프록시가 기본형을 돌려주는 메서드를 물어봐도 터지지 않도록 */
    private static Object blank(Class<?> type) {
        if (!type.isPrimitive() || type == void.class) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == long.class) {
            return 0L;
        }
        return 0;
    }
}

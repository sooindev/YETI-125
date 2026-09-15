package com.irion.testsupport;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletMapping;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.MappingMatch;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * 서블릿 가짜 객체. 목 프레임워크 없이 JDK 동적 Proxy 로.
 * 필터·인터셉터 테스트 공용이라 testsupport 에 둔다
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
        private String remoteAddr = "127.0.0.1";   // 평소에는 nginx 를 거쳐 들어온다
        private final Map<String, String> headers = new HashMap<String, String>();
        private final Map<String, String> params = new HashMap<String, String>();

        /** 요청 속성. 스프링이 계산한 경로를 여기 캐시한다 */
        private final Map<String, Object> attributes = new HashMap<String, Object>();

        /** getSession(true) 호출 여부 — 필터가 세션을 만들면 안 된다 */
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

        /** 톰캣이 보는 주소. 프록시를 거치면 nginx 주소 */
        public Request remoteAddr(String value) {
            this.remoteAddr = value;
            return this;
        }

        public Request param(String name, String value) {
            this.params.put(name, value);
            return this;
        }

        /** AJAX 요청 */
        public Request ajax() {
            return header("X-Requested-With", "XMLHttpRequest").header("Accept", "application/json");
        }

        /** 주소창 진입 요청 */
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
                            case "getRemoteAddr":
                                return remoteAddr;
                            case "getHeader":
                                return headers.get((String) args[0]);
                            case "getParameter":
                                return params.get((String) args[0]);
                            // 서블릿 4.0. 스프링 UrlPathHelper 가 서블릿 매핑을 여기서 묻는다 —
                            // null 이면 핸들러 매핑이 NPE. DispatcherServlet 이 "/" 에 걸린 상태
                            case "getAttribute":
                                return attributes.get((String) args[0]);
                            case "setAttribute":
                                attributes.put((String) args[0], args[1]);
                                return null;
                            case "removeAttribute":
                                attributes.remove((String) args[0]);
                                return null;
                            case "getHttpServletMapping":
                                return rootServletMapping();
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


    /** 로그인 전 — 빈 세션 */
    public static HttpSession session() {
        return session(new HashMap<String, Object>());
    }

    /** 관리자 로그인 세션 */
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


    /** DispatcherServlet 이 "/" 에 매핑된 상태(web.xml 과 동일) */
    private static HttpServletMapping rootServletMapping() {
        return (HttpServletMapping) Proxy.newProxyInstance(
                HttpServletMapping.class.getClassLoader(),
                new Class<?>[] { HttpServletMapping.class },
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getMappingMatch": return MappingMatch.DEFAULT;
                        case "getPattern":      return "/";
                        case "getServletName":  return "dispatcher";
                        case "getMatchValue":   return "";
                        default:                return blank(method.getReturnType());
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

        /** 301 은 sendRedirect 가 아니라 setStatus + Location */
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
                            case "sendError":
                                // sendError 두 오버로드 모두 첫 값이 상태 코드
                                status = (Integer) args[0];
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
        /** 필터가 요청을 통과시켰는지 */
        public boolean passed;

        public FilterChain build() {
            return (request, response) -> passed = true;
        }
    }

    /** 기본형 반환 메서드에도 프록시가 터지지 않도록 */
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

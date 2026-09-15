package com.irion.common.web;

/**
 * 서버 렌더링용 이스케이프.
 *
 * JSTL 이 없어 ${...} 는 받은 글자를 그대로 찍는다 —
 * 우리가 쓰지 않은 값(클립 제목 등)은 반드시 여기를 거쳐야 한다.
 * 브라우저 쪽 짝은 YetiUtil.escapeHtml. 규칙은 같아야 한다
 */
public final class Escape {

    private Escape() {
    }

    /** HTML 본문·속성값 공용. 속성은 반드시 큰따옴표로 */
    public static String html(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        StringBuilder out = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&':  out.append("&amp;");  break;
                case '<':  out.append("&lt;");   break;
                case '>':  out.append("&gt;");   break;
                case '"':  out.append("&quot;"); break;
                case '\'': out.append("&#039;"); break;
                default:   out.append(c);
            }
        }
        return out.toString();
    }

    /**
     * JSON 문자열 안쪽용. 감싸는 따옴표는 호출 쪽 몫.
     *
     * '<' 까지 \\u003c 로 바꾸는 것이 핵심 — 이 값은 ld+json 스크립트 안에 들어가고,
     * 제목에 "&lt;/script&gt;" 가 있으면 HTML 파서가 거기서 스크립트를 닫는다.
     * JSON 문법만으로는 못 막는다
     */
    public static String json(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        StringBuilder out = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':  out.append("\\\"");   break;
                case '\\': out.append("\\\\");   break;
                case '\n': out.append("\\n");    break;
                case '\r': out.append("\\r");    break;
                case '\t': out.append("\\t");    break;
                case '<':  out.append("\\u003c"); break;
                case '>':  out.append("\\u003e"); break;
                case '&':  out.append("\\u0026"); break;
                default:
                    // 제어문자는 JSON 에 날것으로 못 옴
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
            }
        }
        return out.toString();
    }

    /**
     * XML 텍스트 노드용(사이트맵 &lt;loc&gt;).
     * HTML 과 달리 &#039; 같은 이름 실체를 못 써서 &apos; 로
     */
    public static String xml(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        StringBuilder out = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&':  out.append("&amp;");  break;
                case '<':  out.append("&lt;");   break;
                case '>':  out.append("&gt;");   break;
                case '"':  out.append("&quot;"); break;
                case '\'': out.append("&apos;"); break;
                default:   out.append(c);
            }
        }
        return out.toString();
    }
}

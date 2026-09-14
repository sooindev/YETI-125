package com.irion.common.web;

/**
 * 서버에서 그리는 값에 씌우는 이스케이프.
 *
 * 이 저장소의 JSP 에는 JSTL 이 없다 — <c:out> 이 없으니 ${...} 는 받은 글자를 그대로 찍는다.
 * 화면에 나가는 값 가운데 <b>우리가 쓰지 않은 글자</b>(치지직 클립 제목이 그렇다. 클립을 딴
 * 시청자가 붙인 이름이다)는 반드시 여기를 지나야 한다.
 *
 * 브라우저 쪽 짝은 YetiUtil.escapeHtml 이다. 두 곳에 있는 이유는 그리는 자리가 둘이기 때문이고,
 * 규칙은 같아야 한다.
 */
public final class Escape {

    private Escape() {
    }

    /** HTML 본문과 속성값에 함께 쓴다. 속성은 반드시 큰따옴표로 감쌀 것 */
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
     * JSON 문자열 <b>안쪽</b>에 넣을 값. 감싸는 따옴표는 부르는 쪽이 붙인다.
     *
     * '<' 까지 \\u003c 로 바꾸는 것이 핵심이다. 이 값은 &lt;script type="application/ld+json"&gt;
     * 안에 들어가는데, 제목에 "&lt;/script&gt;" 가 들어 있으면 HTML 파서가 거기서 스크립트를
     * 닫아 버린다 — JSON 문법만 맞춰서는 막지 못한다.
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
                    // 제어문자는 JSON 에서 날것으로 올 수 없다
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
     * XML 텍스트 노드용. 사이트맵의 &lt;loc&gt; 이 쓴다.
     * HTML 과 달리 &#039; 같은 이름 있는 실체를 쓸 수 없어 숫자 참조로 적는다.
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

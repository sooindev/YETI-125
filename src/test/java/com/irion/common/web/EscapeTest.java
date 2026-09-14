package com.irion.common.web;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 서버가 그리는 값의 이스케이프.
 *
 * 이 클래스가 필요한 이유는 JSP 에 JSTL 이 없어서다 — ${...} 는 받은 글자를 그대로 찍는다.
 * 클립 제목은 클립을 딴 시청자가 붙인 이름이라 우리가 쓴 글이 아니고,
 * 그 글자가 화면 세 군데(HTML · JSON-LD · 사이트맵 XML)로 각각 다른 규칙으로 나간다.
 */
public class EscapeTest {

    // ── HTML ──────────────────────────────────────────────

    @Test
    public void 태그를_열지_못하게_한다() {
        assertEquals("&lt;script&gt;alert(1)&lt;/script&gt;",
                Escape.html("<script>alert(1)</script>"));
    }

    @Test
    public void 속성을_빠져나가지_못하게_한다() {
        // 큰따옴표로 감싼 속성값 안에서 따옴표를 닫고 onerror 를 붙이는 수법
        assertEquals("&quot; onerror=&quot;alert(1)",
                Escape.html("\" onerror=\"alert(1)"));
    }

    @Test
    public void 앰퍼샌드를_먼저_바꾼다() {
        // & 를 나중에 바꾸면 &lt; 가 &amp;lt; 로 두 번 먹힌다
        assertEquals("&amp;lt;", Escape.html("&lt;"));
    }

    @Test
    public void 빈_값과_null_은_빈_문자열이다() {
        assertEquals("", Escape.html(null));
        assertEquals("", Escape.html(""));
    }

    @Test
    public void 한글과_이모지는_그대로_둔다() {
        assertEquals("이리온 클립 🎮", Escape.html("이리온 클립 🎮"));
    }

    // ── JSON ──────────────────────────────────────────────

    /**
     * 이 파일에서 제일 중요한 약속.
     *
     * JSON 문법만 맞추면 "</script>" 가 날것으로 남는데, 그 값은
     * <script type="application/ld+json"> 안에 들어간다 — HTML 파서가 거기서 스크립트를 닫는다.
     */
    @Test
    public void JSON_안에서도_스크립트를_닫지_못한다() {
        String escaped = Escape.json("</script><img src=x onerror=alert(1)>");

        assertFalse("'<' 가 날것으로 남으면 안 된다", escaped.contains("<"));
        assertTrue(escaped.contains("\\u003c"));
    }

    @Test
    public void JSON_따옴표와_역슬래시를_막는다() {
        assertEquals("\\\"큰따옴표\\\"", Escape.json("\"큰따옴표\""));
        assertEquals("C:\\\\경로", Escape.json("C:\\경로"));
    }

    @Test
    public void JSON_줄바꿈은_문자열을_끊지_않는다() {
        assertEquals("첫 줄\\n둘째 줄", Escape.json("첫 줄\n둘째 줄"));
    }

    @Test
    public void JSON_제어문자는_유니코드로_적는다() {
        assertEquals("\\u0000", Escape.json("\u0000"));
    }

    // ── XML ───────────────────────────────────────────────

    @Test
    public void XML_은_작은따옴표를_이름있는_실체로_쓰지_않는다() {
        // &#039; 는 XML 에 없는 이름이다 — 사이트맵 파서가 깨진다
        assertEquals("&apos;", Escape.xml("'"));
    }

    @Test
    public void XML_에서도_태그를_열지_못한다() {
        assertEquals("&lt;loc&gt;", Escape.xml("<loc>"));
    }
}

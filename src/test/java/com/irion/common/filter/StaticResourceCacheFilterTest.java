package com.irion.common.filter;

import com.irion.testsupport.FakeHttp;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * 보안 헤더와 캐시 정책을 못 박는다.
 *
 * 이 필터는 한 번 세워두면 아무도 다시 안 보는 종류라, 방어가 풀려도 화면은 멀쩡하다.
 * CSP 에서 'unsafe-inline' 하나만 되돌아와도 인라인 스크립트 방어가 통째로 사라지는데
 * 그때 깨지는 것이 아무것도 없다 — 그래서 여기서 깨뜨린다.
 */
public class StaticResourceCacheFilterTest {

    // ── 보안 헤더 ──────────────────────────────────────────────

    /** 페이지든 이미지든 가리지 않고 붙어야 한다 */
    @Test
    public void 보안_헤더는_모든_응답에_붙는다() throws Exception {
        for (String uri : new String[] { "/", "/schedule", "/resources/css/common.css",
                "/resources/images/snowflake.png", "/admin/admin-login" }) {

            FakeHttp.Response response = run(uri);

            assertNotNull(uri + " 에 CSP 가 없다", response.header("Content-Security-Policy"));
            assertEquals(uri + " 의 X-Frame-Options 가 다르다",
                    "DENY", response.header("X-Frame-Options"));
            assertEquals(uri + " 의 X-Content-Type-Options 가 다르다",
                    "nosniff", response.header("X-Content-Type-Options"));
            assertNotNull(uri + " 에 HSTS 가 없다",
                    response.header("Strict-Transport-Security"));
            assertNotNull(uri + " 에 Referrer-Policy 가 없다",
                    response.header("Referrer-Policy"));
        }
    }

    /**
     * 이 프로젝트에서 제일 되돌리기 쉬운 방어다.
     * 닫기 버튼 하나를 onclick 으로 되돌리면 CSP 에 'unsafe-inline' 을 넣고 싶어진다.
     * 그러면 인라인 스크립트를 막는 의미가 없어지므로, 그 순간 여기가 깨져야 한다.
     */
    @Test
    public void script_src_에_인라인_허용이_들어오면_깨진다() throws Exception {
        String scriptSrc = directive(csp(), "script-src");

        assertNotNull("CSP 에 script-src 가 없다 — 없으면 default-src 로 흘러간다", scriptSrc);
        assertFalse("script-src 에 'unsafe-inline' 이 들어왔다. 인라인 핸들러를 되살리는 대신 "
                + "data-* 속성과 이벤트 위임으로 붙일 것: " + scriptSrc,
                scriptSrc.contains("'unsafe-inline'"));
        assertFalse("script-src 에 'unsafe-eval' 이 들어왔다: " + scriptSrc,
                scriptSrc.contains("'unsafe-eval'"));
    }

    /** 와일드카드로 열어두면 SRI 를 걸어둔 의미가 없다 */
    @Test
    public void script_src_는_출처를_열거한다() throws Exception {
        String scriptSrc = directive(csp(), "script-src");

        assertFalse("script-src 가 * 로 열려 있다: " + scriptSrc, scriptSrc.contains("*"));
        assertTrue("script-src 에 'self' 가 없다: " + scriptSrc, scriptSrc.contains("'self'"));
    }

    /** 빠지면 조용히 느슨해지는 지시자들 — 값까지 함께 본다 */
    @Test
    public void 잠가둔_지시자는_그대로여야_한다() throws Exception {
        String csp = csp();

        assertEquals("default-src", "'self'", directive(csp, "default-src"));
        assertEquals("object-src — 플러그인 차단", "'none'", directive(csp, "object-src"));
        assertEquals("base-uri — <base> 로 상대경로를 납치하는 것을 막는다",
                "'self'", directive(csp, "base-uri"));
        assertEquals("form-action — 폼이 남의 서버로 나가는 것을 막는다",
                "'self'", directive(csp, "form-action"));
        assertEquals("frame-ancestors — X-Frame-Options 를 모르는 브라우저용",
                "'none'", directive(csp, "frame-ancestors"));
    }

    /** 클립 임베드가 도는 유일한 출처. 넓히면 아무 페이지나 우리 안에서 열린다 */
    @Test
    public void frame_src_는_치지직만_허용한다() throws Exception {
        assertEquals("https://chzzk.naver.com", directive(csp(), "frame-src"));
    }

    // ── 캐시 ──────────────────────────────────────────────────

    /**
     * 확장자 없는 페이지 주소(/schedule)가 빠지기 쉽다.
     * 빠지면 브라우저 휴리스틱 캐시가 새 HTML 과 옛 CSS 를 섞는다.
     */
    @Test
    public void 페이지와_css_js_는_재검증하게_한다() throws Exception {
        for (String uri : new String[] { "/", "/schedule", "/info", "/admin/schedule",
                "/index.html", "/resources/css/common.css", "/resources/js/index.js" }) {

            assertEquals(uri + " 에 Cache-Control 이 없다",
                    "no-cache", run(uri).header("Cache-Control"));
        }
    }

    /** 이미지는 안 바뀌므로 매번 되묻지 않는다 */
    @Test
    public void 이미지는_재검증에서_뺀다() throws Exception {
        for (String uri : new String[] { "/resources/images/snowflake.png",
                "/resources/images/Irion-avatar.webp", "/resources/images/Irion-profile.jpg" }) {

            assertNull(uri + " 에 Cache-Control 이 붙었다", run(uri).header("Cache-Control"));
        }
    }

    /** 헤더만 붙이고 요청은 그대로 흘려보내야 한다 */
    @Test
    public void 요청을_가로막지_않는다() throws Exception {
        FakeHttp.Chain chain = new FakeHttp.Chain();
        new StaticResourceCacheFilter().doFilter(
                new FakeHttp.Request().uri("/schedule").build(),
                new FakeHttp.Response().build(),
                chain.build());

        assertTrue("필터가 요청을 다음으로 넘기지 않았다", chain.passed);
    }

    // ── 매핑 ──────────────────────────────────────────────────

    /**
     * 필터 기본값은 REQUEST 뿐이다. ERROR 를 안 적으면 404·500 페이지만
     * 위의 헤더를 하나도 못 받는다 — 코드로는 드러나지 않아 여기서 본다.
     */
    @Test
    public void 에러_페이지에도_필터가_돌도록_매핑돼_있다() throws Exception {
        String mapping = cacheFilterMapping();

        assertTrue("staticResourceCacheFilter 매핑에 <dispatcher>ERROR</dispatcher> 가 없다. "
                + "그러면 404·500 페이지가 CSP 없이 나간다:\n" + mapping,
                mapping.contains("<dispatcher>ERROR</dispatcher>"));
        assertTrue("<dispatcher> 를 쓰면 REQUEST 기본값이 사라진다 — 함께 적어야 한다:\n" + mapping,
                mapping.contains("<dispatcher>REQUEST</dispatcher>"));
    }


    // ── 거들 ──────────────────────────────────────────────────

    /** 필터가 실제로 내보낸 CSP 를 읽는다 — 상수를 직접 보면 필터가 안 붙여도 통과한다 */
    private static String csp() throws Exception {
        return run("/").header("Content-Security-Policy");
    }

    private static FakeHttp.Response run(String uri) throws Exception {
        FakeHttp.Response response = new FakeHttp.Response();
        new StaticResourceCacheFilter().doFilter(
                new FakeHttp.Request().uri(uri).build(),
                response.build(),
                new FakeHttp.Chain().build());
        return response;
    }

    /** "script-src 'self' https://..." 에서 이름 뒤의 값만. 없으면 null */
    private static String directive(String csp, String name) {
        for (String part : csp.split(";")) {
            String trimmed = part.trim();
            if (trimmed.equals(name)) {
                return "";
            }
            if (trimmed.startsWith(name + " ")) {
                return trimmed.substring(name.length() + 1).trim();
            }
        }
        return null;
    }

    /** web.xml 에서 staticResourceCacheFilter 의 filter-mapping 블록만 잘라낸다 */
    private static String cacheFilterMapping() throws Exception {
        File webXml = new File("src/main/webapp/WEB-INF/web.xml");
        assertTrue("web.xml 을 찾지 못했다: " + webXml.getAbsolutePath(), webXml.isFile());

        String xml = new String(Files.readAllBytes(webXml.toPath()), StandardCharsets.UTF_8);

        Matcher matcher = Pattern.compile(
                "<filter-mapping>(?:(?!</filter-mapping>).)*?staticResourceCacheFilter.*?</filter-mapping>",
                Pattern.DOTALL).matcher(xml);

        assertTrue("web.xml 에 staticResourceCacheFilter 의 filter-mapping 이 없다", matcher.find());
        return matcher.group();
    }
}

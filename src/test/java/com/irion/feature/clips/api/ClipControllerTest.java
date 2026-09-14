package com.irion.feature.clips.api;

import com.irion.integration.chzzk.LiveFeedService;
import com.irion.testsupport.FakeHttp;
import org.junit.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletResponse;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * 클립 상세 화면이 서버에서 그려지기 직전의 값.
 *
 * 이 화면은 이 저장소에서 유일하게 <b>바깥에서 온 글자를 서버가 HTML 에 박는</b> 곳이다.
 * JSTL 이 없어 JSP 가 대신 막아 주지 않으므로, 여기서 이스케이프를 마쳤는지가 곧 방어다.
 *
 * 함께 못 박는 것 하나 더 — 우리 채널 목록에 없는 clipId 는 404 여야 한다.
 * 치지직 단건 API 는 채널을 알려주지 않아, 그것만 믿으면 남의 클립이 우리 주소로 열린다.
 */
public class ClipControllerTest {

    // ── 주소 모양 ─────────────────────────────────────────

    /**
     * clipId 의 모양을 매핑에서 거른다. 여기 걸리지 않는 주소는 아무 핸들러에도 닿지 않아 404 다.
     *
     * 이 테스트가 있는 이유는 패턴 안에 중괄호가 또 들어 있기 때문이다 —
     * {clipId:[A-Za-z0-9_-]{1,64}} 의 {1,64} 를 스프링이 경로 변수의 끝으로 읽어 버리면
     * 매핑이 조용히 엉뚱한 것을 받는다.
     */
    @Test
    public void 주소의_clipId_모양을_매핑에서_거른다() throws Exception {
        String pattern = "/clips" + ClipController.class
                .getDeclaredMethod("detail", String.class, Model.class, HttpServletResponse.class)
                .getAnnotation(GetMapping.class).value()[0];

        AntPathMatcher matcher = new AntPathMatcher();

        assertTrue("치지직 clipUID 는 영숫자다", matcher.match(pattern, "/clips/kJ3y4mwyqe"));
        assertTrue("밑줄과 붙임표도 받는다", matcher.match(pattern, "/clips/a-b_c"));

        assertFalse("경로를 거슬러 올라가지 못한다", matcher.match(pattern, "/clips/../info"));
        assertFalse("점은 받지 않는다", matcher.match(pattern, "/clips/a.jsp"));
        assertFalse("한글은 받지 않는다", matcher.match(pattern, "/clips/한글"));
        assertFalse("빈 값은 목록 주소다", matcher.match(pattern, "/clips/"));

        StringBuilder tooLong = new StringBuilder();
        for (int i = 0; i < 65; i++) {
            tooLong.append('a');
        }
        assertFalse("64자를 넘으면 받지 않는다", matcher.match(pattern, "/clips/" + tooLong));
    }

    // ── 소유 확인 ─────────────────────────────────────────

    @Test
    public void 목록을_다_받았는데_없으면_404_다() throws Exception {
        ClipController controller = controllerWith();   // 아무것도 못 찾는다
        FakeHttp.Response response = new FakeHttp.Response();
        Model model = new ExtendedModelMap();

        String view = controller.detail("남의클립", model, response.build());

        assertNull("404 를 낸 뒤에는 화면을 고르지 않는다", view);
        assertEquals(404, response.status);
    }

    /**
     * 이 저장소에서 제일 비싼 실수를 막는 자리.
     *
     * 캐시가 막 만료돼 목록을 다시 채우는 중이거나 치지직이 죽었을 때도 클립을 못 찾는다.
     * 그때 404 를 내면 멀쩡한 주소를 검색엔진이 "사라졌다" 로 읽고 색인에서 지운다 —
     * 이 기능을 만든 이유가 색인인데 그것을 스스로 무너뜨린다.
     */
    @Test
    public void 목록을_덜_받았으면_404_가_아니라_503_이다() throws Exception {
        ClipController controller = controllerUnsure();
        FakeHttp.Response response = new FakeHttp.Response();
        Model model = new ExtendedModelMap();

        String view = controller.detail("아직모름", model, response.build());

        assertNull(view);
        assertEquals("없다고 단정하면 안 된다", 503, response.status);
        assertEquals("잠시 뒤 다시 오라고 알린다", "30", response.header("Retry-After"));
    }

    @Test
    public void 목록을_덜_받았어도_찾았으면_그대로_보여준다() throws Exception {
        ClipController controller = controllerUnsure(clip("abc123", "전문가 의사"));
        FakeHttp.Response response = new FakeHttp.Response();
        Model model = new ExtendedModelMap();

        String view = controller.detail("abc123", model, response.build());

        assertEquals("pages/clip-detail", view);
        assertEquals(200, response.status);
    }

    @Test
    public void 목록에_있는_클립은_상세_화면을_고른다() throws Exception {
        ClipController controller = controllerWith(clip("abc123", "전문가 의사"));
        FakeHttp.Response response = new FakeHttp.Response();
        Model model = new ExtendedModelMap();

        String view = controller.detail("abc123", model, response.build());

        assertEquals("pages/clip-detail", view);
        assertEquals(200, response.status);
        assertEquals("전문가 의사", model.asMap().get("title"));
    }

    // ── 이스케이프 ────────────────────────────────────────

    @Test
    public void 제목의_태그는_화면에_박히기_전에_막힌다() {
        Map<String, Object> model = view(clip("abc123", "<img src=x onerror=alert(1)>"));

        String title = (String) model.get("title");
        assertFalse("HTML 로 나가는 제목에 '<' 가 남으면 안 된다", title.contains("<"));
        assertTrue(title.contains("&lt;img"));
    }

    @Test
    public void 제목이_JSON_LD_를_빠져나가지_못한다() {
        Map<String, Object> model = view(clip("abc123", "</script>망가뜨리기"));

        String titleJson = (String) model.get("titleJson");
        assertFalse(titleJson.contains("<"));
        assertTrue(titleJson.contains("\\u003c"));
    }

    /** 설명(meta description)은 제목을 큰따옴표로 감싸 만든다 — 제목의 따옴표가 그대로 새면 속성이 닫힌다 */
    @Test
    public void 설명에도_제목이_들어가므로_함께_막는다() {
        Map<String, Object> model = view(clip("abc123", "\"따옴표\""));

        assertFalse("HTML 속성 안에 날것의 따옴표가 남으면 안 된다",
                ((String) model.get("description")).contains("\""));
        assertTrue(((String) model.get("description")).contains("&quot;"));

        // JSON 쪽은 지우는 것이 아니라 역슬래시를 앞세워 살려 둔다
        assertTrue(((String) model.get("descriptionJson")).contains("\\\""));
    }

    // ── 썸네일 ────────────────────────────────────────────

    @Test
    public void https_가_아닌_썸네일은_버린다() {
        Map<String, Object> clip = clip("abc123", "제목");
        clip.put("thumbnailUrl", "javascript:alert(1)");

        Map<String, Object> model = view(clip);

        assertTrue("주소가 아니라 자리를 채우는 투명 이미지가 들어간다",
                ((String) model.get("thumbnailUrl")).startsWith("data:image/gif;base64,"));
        assertEquals(" hidden", model.get("thumbnailHidden"));
        assertEquals("", model.get("fallbackHidden"));
    }

    @Test
    public void 썸네일이_있으면_대체_자리를_끈다() {
        Map<String, Object> clip = clip("abc123", "제목");
        clip.put("thumbnailUrl", "https://video-phinf.pstatic.net/a.jpg");

        Map<String, Object> model = view(clip);

        assertEquals("https://video-phinf.pstatic.net/a.jpg", model.get("thumbnailUrl"));
        assertEquals("", model.get("thumbnailHidden"));
        assertEquals(" hidden", model.get("fallbackHidden"));
        assertEquals("공유 미리보기도 같은 그림이라야 한다",
                "https://video-phinf.pstatic.net/a.jpg", model.get("ogImage"));
    }

    @Test
    public void 썸네일이_없으면_공유_미리보기는_사이트_얼굴로_대신한다() {
        Map<String, Object> model = view(clip("abc123", "제목"));

        assertEquals("https://yeti-125.com/resources/images/profile/Irion-profile.jpg",
                model.get("ogImage"));
    }

    // ── 연령 제한 ─────────────────────────────────────────

    @Test
    public void 연령_제한_클립은_색인에서_뺀다() {
        Map<String, Object> clip = clip("abc123", "제목");
        clip.put("adult", Boolean.TRUE);

        Map<String, Object> model = view(clip);

        assertEquals("noindex, follow", model.get("robots"));
        assertEquals("연령 제한", model.get("fallbackNote"));
        assertEquals("19 배지를 켠다", "", model.get("fallbackBadgeHidden"));
    }

    @Test
    public void 보통_클립은_색인한다() {
        Map<String, Object> model = view(clip("abc123", "제목"));

        assertEquals("index, follow", model.get("robots"));
        assertEquals(" hidden", model.get("fallbackBadgeHidden"));
    }

    // ── 숫자·날짜 다듬기 ──────────────────────────────────

    @Test
    public void 길이를_사람이_읽는_모양과_ISO_둘로_낸다() {
        assertEquals("0:15", ClipController.ClipView.duration(15));
        assertEquals("PT15S", ClipController.ClipView.isoDuration(15));

        assertEquals("2:05", ClipController.ClipView.duration(125));
        assertEquals("PT2M5S", ClipController.ClipView.isoDuration(125));

        // 한 시간을 넘기는 클립은 없지만, 없다고 믿고 쓰면 언젠가 "65:00" 이 나온다
        assertEquals("1:01:05", ClipController.ClipView.duration(3665));
        assertEquals("PT1H1M5S", ClipController.ClipView.isoDuration(3665));
    }

    @Test
    public void 길이가_없으면_0_으로_본다() {
        assertEquals("0:00", ClipController.ClipView.duration(0));
        assertEquals("PT0S", ClipController.ClipView.isoDuration(0));
    }

    @Test
    public void 만든_날을_한국어와_ISO_둘로_낸다() {
        assertEquals("2025년 7월 30일", ClipController.ClipView.koreanDate("2025-07-30 21:54:50"));
        assertEquals("2025-07-30T21:54:50+09:00", ClipController.ClipView.isoDate("2025-07-30 21:54:50"));
    }

    /** 치지직이 주는 모양이 바뀌어도 화면 전체가 무너지지는 않아야 한다 */
    @Test
    public void 날짜_모양이_다르면_날짜_줄만_빈다() {
        assertEquals("", ClipController.ClipView.koreanDate("어제"));
        assertEquals("", ClipController.ClipView.isoDate("2025-07-30"));
        assertEquals("", ClipController.ClipView.koreanDate(""));
    }

    @Test
    public void 조회수는_천단위로_끊어_읽힌다() {
        assertEquals("31,389", ClipController.ClipView.comma(31389));
    }

    @Test
    public void 숫자가_아닌_조회수는_0_으로_본다() {
        Map<String, Object> clip = clip("abc123", "제목");
        clip.put("viewCount", "많음");

        Map<String, Object> model = view(clip);

        assertEquals("0", model.get("viewCountText"));
        assertEquals(0L, model.get("viewCount"));
    }

    // ── 도우미 ────────────────────────────────────────────

    private static Map<String, Object> view(Map<String, Object> clip) {
        return new ClipController.ClipView(clip).asModel();
    }

    private static Map<String, Object> clip(String id, String title) {
        Map<String, Object> clip = new HashMap<String, Object>();
        clip.put("clipId", id);
        clip.put("clipTitle", title);
        clip.put("viewCount", "31389");
        clip.put("duration", "15");
        clip.put("createdAt", "2025-07-30 21:54:50");
        clip.put("clipUrl", "https://chzzk.naver.com/clips/" + id);
        clip.put("adult", Boolean.FALSE);
        return clip;
    }

    /** 아직 목록을 다 받지 못한 캐시 — 못 찾아도 "없다" 고 단정하지 못한다 */
    @SafeVarargs
    private static ClipController controllerUnsure(final Map<String, Object>... clips) throws Exception {
        final Map<String, Map<String, Object>> known = index(clips);

        return inject(new LiveFeedService() {
            @Override
            public ClipLookup findClip(String clipId) {
                return lookup(known.get(clipId), false);
            }
        });
    }

    /** 패키지 밖에서는 ClipLookup 을 만들 수 없다 — 생성자가 패키지 전용이라 리플렉션으로 짓는다 */
    private static LiveFeedService.ClipLookup lookup(Map<String, Object> clip, boolean complete) {
        try {
            java.lang.reflect.Constructor<LiveFeedService.ClipLookup> constructor =
                    LiveFeedService.ClipLookup.class.getDeclaredConstructor(Map.class, boolean.class);
            constructor.setAccessible(true);
            return constructor.newInstance(clip, complete);
        } catch (Exception e) {
            throw new IllegalStateException("ClipLookup 을 만들지 못했다", e);
        }
    }

    private static Map<String, Map<String, Object>> index(Map<String, Object>[] clips) {
        Map<String, Map<String, Object>> known = new HashMap<String, Map<String, Object>>();
        for (Map<String, Object> clip : clips) {
            known.put((String) clip.get("clipId"), clip);
        }
        return known;
    }

    private static ClipController inject(LiveFeedService feed) throws Exception {
        ClipController controller = new ClipController();
        Field field = ClipController.class.getDeclaredField("liveFeed");
        field.setAccessible(true);
        field.set(controller, feed);
        return controller;
    }

    /** 주어진 클립만 아는 캐시를 물린 컨트롤러 */
    @SafeVarargs
    private static ClipController controllerWith(final Map<String, Object>... clips) throws Exception {
        final Map<String, Map<String, Object>> known = index(clips);

        return inject(new LiveFeedService() {
            @Override
            public ClipLookup findClip(String clipId) {
                // 아는 목록이 전부다 — 못 찾으면 정말로 없는 것이다
                return lookup(known.get(clipId), true);
            }
        });
    }
}

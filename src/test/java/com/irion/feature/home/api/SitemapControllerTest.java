package com.irion.feature.home.api;

import com.irion.integration.chzzk.ClipFeeds;
import com.irion.integration.chzzk.LiveFeedService;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 사이트맵.
 *
 * 손으로 적던 파일을 없애고 캐시에서 뽑게 바꾼 자리다. 지켜야 할 것이 셋 있다 —
 * 고정 주소가 빠지지 않을 것, 연령 제한 클립이 실리지 않을 것(상세 화면이 noindex 라 서로 어긋난다),
 * 그리고 치지직이 죽어도 500 이 아니라 짧은 사이트맵이 나갈 것.
 */
public class SitemapControllerTest {

    @Test
    public void 고정_주소_넷이_들어간다() throws Exception {
        String xml = sitemapWith(ClipFeeds.of());

        assertTrue(xml.contains("<loc>https://yeti-125.com/</loc>"));
        assertTrue(xml.contains("<loc>https://yeti-125.com/clips</loc>"));
        assertTrue(xml.contains("<loc>https://yeti-125.com/schedule</loc>"));
        assertTrue(xml.contains("<loc>https://yeti-125.com/info</loc>"));
    }

    @Test
    public void 클립마다_주소가_하나씩_실린다() throws Exception {
        String xml = sitemapWith(ClipFeeds.of(
                ClipFeeds.clip("aaa", "첫 클립", "2025-07-30 21:54:50", false),
                ClipFeeds.clip("bbb", "둘째 클립", "2026-01-02 10:00:00", false)));

        assertTrue(xml.contains("<loc>https://yeti-125.com/clips/aaa</loc>"));
        assertTrue(xml.contains("<loc>https://yeti-125.com/clips/bbb</loc>"));
    }

    @Test
    public void 만든_날이_lastmod_가_된다() throws Exception {
        String xml = sitemapWith(ClipFeeds.of(
                ClipFeeds.clip("aaa", "첫 클립", "2025-07-30 21:54:50", false)));

        assertTrue(xml.contains("<lastmod>2025-07-30</lastmod>"));
    }

    /** 날짜 모양이 바뀌어도 사이트맵 전체가 깨지면 안 된다 — 그 항목의 lastmod 만 빠진다 */
    @Test
    public void 날짜를_못_읽으면_lastmod_를_아예_뺀다() throws Exception {
        String xml = sitemapWith(ClipFeeds.of(
                ClipFeeds.clip("aaa", "첫 클립", "어제", false)));

        assertTrue(xml.contains("<loc>https://yeti-125.com/clips/aaa</loc>"));
        assertFalse(xml.contains("<lastmod>"));
    }

    @Test
    public void 연령_제한_클립은_싣지_않는다() throws Exception {
        String xml = sitemapWith(ClipFeeds.of(
                ClipFeeds.clip("aaa", "보통 클립", "2025-07-30 21:54:50", false),
                ClipFeeds.clip("bbb", "19금 클립", "2025-07-30 21:54:50", true)));

        assertTrue(xml.contains("/clips/aaa"));
        assertFalse("상세 화면이 noindex 인 주소를 사이트맵에 넣으면 서로 어긋난다",
                xml.contains("/clips/bbb"));
    }

    @Test
    public void clipId_가_없는_항목은_건너뛴다() throws Exception {
        String xml = sitemapWith(ClipFeeds.of(
                ClipFeeds.clip(null, "이름 없는 클립", "2025-07-30 21:54:50", false)));

        assertFalse(xml.contains("/clips/null"));
    }

    /**
     * 치지직이 죽었을 때. 사이트맵이 500 을 내면 검색엔진이 "이 사이트의 사이트맵은 고장났다"로
     * 기억하므로, 짧게라도 성한 것을 준다.
     */
    @Test
    public void 클립을_못_받아도_고정_주소는_나간다() throws Exception {
        String xml = sitemapWith(null);

        assertTrue(xml.contains("<loc>https://yeti-125.com/</loc>"));
        assertTrue(xml.contains("</urlset>"));
        assertFalse(xml.contains("/clips/"));
    }

    @Test
    public void 열고_닫는_짝이_맞는다() throws Exception {
        String xml = sitemapWith(ClipFeeds.of(
                ClipFeeds.clip("aaa", "첫 클립", "2025-07-30 21:54:50", false)));

        assertTrue(xml.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        assertEquals(count(xml, "<url>"), count(xml, "</url>"));
        assertTrue(xml.trim().endsWith("</urlset>"));
    }

    // ── 도우미 ────────────────────────────────────────────

    private static int count(String haystack, String needle) {
        int found = 0;
        for (int at = haystack.indexOf(needle); at >= 0; at = haystack.indexOf(needle, at + 1)) {
            found++;
        }
        return found;
    }

    private static String sitemapWith(final LiveFeedService.ClipFeed feed) throws Exception {
        LiveFeedService liveFeed = new LiveFeedService() {
            @Override
            public ClipFeed getAllClips() {
                return feed;
            }
        };

        SitemapController controller = new SitemapController();
        Field field = SitemapController.class.getDeclaredField("liveFeed");
        field.setAccessible(true);
        field.set(controller, liveFeed);
        return controller.sitemap();
    }
}

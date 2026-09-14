package com.irion.feature.home.api;

import com.irion.common.web.Escape;
import com.irion.common.web.Site;
import com.irion.integration.chzzk.LiveFeedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

/**
 * 사이트맵. 전에는 webapp 에 놓인 고정 파일이었다.
 *
 * 클립마다 주소가 생기면서 손으로 적을 수 있는 분량을 넘었다 — 1,700개가 넘고,
 * 새 클립은 우리가 모르는 사이에 늘어난다. 캐시가 들고 있는 목록에서 바로 뽑는다.
 *
 * 치지직이 죽어 목록을 못 받으면 고정 주소 넷만 내보낸다. 사이트맵이 500 을 내면
 * 검색엔진이 "이 사이트의 사이트맵은 고장났다"로 기억하므로, 짧게라도 성한 것을 주는 편이 낫다.
 */
@Controller
public class SitemapController {

    @Autowired
    private LiveFeedService liveFeed;

    @GetMapping(value = "/sitemap.xml", produces = "application/xml;charset=UTF-8")
    @ResponseBody
    public String sitemap() {
        StringBuilder xml = new StringBuilder(256 * 1024);

        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
           .append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\"\n")
           .append("        xmlns:image=\"http://www.google.com/schemas/sitemap-image/1.1\">\n");

        page(xml, "/", "daily", "1.0",
                "/resources/images/profile/Irion-profile.jpg", "버추얼 스트리머 이리온(IRION)");
        page(xml, "/clips", "daily", "0.9", null, null);
        page(xml, "/schedule", "daily", "0.9", null, null);
        page(xml, "/info", "weekly", "0.8",
                "/resources/images/profile/Irion-profile.jpg", "이리온(IRION) 프로필");

        appendClips(xml);

        xml.append("</urlset>\n");
        return xml.toString();
    }

    /** 클립 전량. 연령 제한 클립은 뺀다 — 상세 화면도 noindex 라 여기 있으면 서로 어긋난다 */
    private void appendClips(StringBuilder xml) {
        LiveFeedService.ClipFeed feed = liveFeed.getAllClips();
        if (feed == null) {
            return;
        }

        for (Map<String, Object> clip : feed.getClips()) {
            Object id = clip.get("clipId");
            if (!(id instanceof String) || ((String) id).isEmpty()) {
                continue;
            }
            if (Boolean.TRUE.equals(clip.get("adult"))) {
                continue;
            }

            xml.append("  <url>\n")
               .append("    <loc>").append(Escape.xml(Site.url("/clips/" + id))).append("</loc>\n");

            String lastmod = day(clip.get("createdAt"));
            if (!lastmod.isEmpty()) {
                // 클립은 만들어진 뒤로 바뀌지 않는다 — 만든 날이 곧 마지막 수정일이다
                xml.append("    <lastmod>").append(lastmod).append("</lastmod>\n");
            }

            xml.append("    <changefreq>yearly</changefreq>\n")
               .append("    <priority>0.5</priority>\n")
               .append("  </url>\n");
        }
    }

    private static void page(StringBuilder xml, String path, String changefreq, String priority,
                             String imagePath, String imageTitle) {

        xml.append("  <url>\n")
           .append("    <loc>").append(Escape.xml(Site.url(path))).append("</loc>\n")
           .append("    <changefreq>").append(changefreq).append("</changefreq>\n")
           .append("    <priority>").append(priority).append("</priority>\n");

        if (imagePath != null) {
            xml.append("    <image:image>\n")
               .append("      <image:loc>").append(Escape.xml(Site.url(imagePath))).append("</image:loc>\n")
               .append("      <image:title>").append(Escape.xml(imageTitle)).append("</image:title>\n")
               .append("    </image:image>\n");
        }

        xml.append("  </url>\n");
    }

    /** "2025-07-30 21:54:50" → "2025-07-30". 모양이 다르면 빈 문자열 — lastmod 를 아예 빼기 위해서다 */
    private static String day(Object createdAt) {
        if (!(createdAt instanceof String)) {
            return "";
        }
        String value = (String) createdAt;
        if (value.length() < 10) {
            return "";
        }
        String date = value.substring(0, 10);
        return date.matches("\\d{4}-\\d{2}-\\d{2}") ? date : "";
    }
}

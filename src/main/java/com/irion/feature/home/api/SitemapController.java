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
 * 사이트맵. 전에는 고정 파일이었으나 클립 주소가 1,700개를 넘어 캐시에서 생성한다.
 *
 * 치지직 장애 시에는 고정 주소 넷만 내보낸다 —
 * 500 을 내면 검색엔진이 "고장난 사이트맵" 으로 기억한다
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

    /** 클립 전량. 연령 제한 제외 — 상세가 noindex 라 여기 있으면 어긋남 */
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
                // 클립은 생성 후 불변 — 만든 날이 곧 수정일
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

    /** "2025-07-30 21:54:50" → "2025-07-30". 형식 불일치 시 빈 문자열(lastmod 생략용) */
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

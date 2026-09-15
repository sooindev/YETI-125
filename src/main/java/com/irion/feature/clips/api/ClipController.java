package com.irion.feature.clips.api;

import com.irion.common.web.Escape;
import com.irion.common.web.Site;
import com.irion.integration.chzzk.LiveFeedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 클립 아카이브. 목록은 브라우저가, 상세는 서버가 그린다.
 *
 * 상세를 서버 렌더링하는 이유 — 검색엔진 색인과 공유 미리보기.
 * 재생은 없음. 치지직 원본으로 이동(다시보기와 동일)
 */
@Controller
@RequestMapping("/clips")
public class ClipController {

    /** clipUID 모양. 불일치 시 핸들러에 닿지 않아 404 */
    private static final String CLIP_ID = "[A-Za-z0-9_-]{1,64}";

    /** 썸네일 자리용 투명 1x1. src 를 비우면 현재 페이지를 이미지로 요청함 */
    private static final String BLANK_PIXEL =
            "data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7";

    /** 썸네일 없을 때의 공유 미리보기 */
    private static final String FALLBACK_OG_IMAGE =
            Site.url("/resources/images/profile/Irion-profile.jpg");

    @Autowired
    private LiveFeedService liveFeed;

    @GetMapping("")
    public String list() {
        return "pages/clips";
    }

    /** 재시도 안내 시간(초) */
    private static final String RETRY_AFTER = "30";

    /**
     * 클립 한 건. 없음 · 삭제됨 · 남의 채널 → 404.
     *
     * 단, 캐시 재적재 중이거나 치지직 장애 시에도 못 찾는다 —
     * 그때 404 를 내면 멀쩡한 주소가 색인에서 지워지므로 503
     */
    @GetMapping("/{clipId:" + CLIP_ID + "}")
    public String detail(@PathVariable String clipId, Model model, HttpServletResponse response)
            throws IOException {

        LiveFeedService.ClipLookup lookup = liveFeed.findClip(clipId);
        Map<String, Object> clip = lookup.getClip();

        if (clip == null) {
            if (lookup.isComplete()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            } else {
                response.setHeader("Retry-After", RETRY_AFTER);
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            }
            return null;
        }

        model.addAllAttributes(new ClipView(clip).asModel());
        return "pages/clip-detail";
    }

    /**
     * 화면이 그대로 찍어도 되게 다듬은 클립.
     * JSTL 이 없어 ${...} 는 받은 글자를 그대로 내보냄 — 이스케이프도 여기서
     */
    static final class ClipView {

        private final Map<String, Object> clip;

        ClipView(Map<String, Object> clip) {
            this.clip = clip;
        }

        Map<String, Object> asModel() {
            String id = text("clipId");
            String title = text("clipTitle");
            String thumbnail = safeImageUrl(text("thumbnailUrl"));
            boolean adult = Boolean.TRUE.equals(clip.get("adult"));
            long views = number("viewCount");
            long seconds = number("duration");

            String pageUrl = Site.url("/clips/" + id);
            String description = description(title, views);

            Map<String, Object> model = new HashMap<String, Object>();

            model.put("clipUrl", Escape.html(text("clipUrl")));
            model.put("pageUrl", pageUrl);

            model.put("title", Escape.html(title));
            model.put("titleJson", Escape.json(title));
            model.put("description", Escape.html(description));
            model.put("descriptionJson", Escape.json(description));

            // 썸네일 없으면 img 숨김 + 대체 자리 표시 (홈 카드와 동일)
            model.put("thumbnailUrl", thumbnail.isEmpty() ? BLANK_PIXEL : Escape.html(thumbnail));
            model.put("thumbnailHidden", thumbnail.isEmpty() ? " hidden" : "");
            model.put("fallbackHidden", thumbnail.isEmpty() ? "" : " hidden");
            model.put("fallbackBadgeHidden", adult ? "" : " hidden");
            model.put("fallbackNote", adult ? "연령 제한" : "No Thumbnail");
            model.put("fallbackLabel", adult ? "연령 제한 콘텐츠 — 썸네일 비공개" : "썸네일 없음");

            model.put("ogImage", thumbnail.isEmpty() ? FALLBACK_OG_IMAGE : Escape.html(thumbnail));

            model.put("viewCount", views);
            model.put("viewCountText", comma(views));
            model.put("durationText", duration(seconds));
            model.put("durationIso", isoDuration(seconds));
            model.put("createdText", koreanDate(text("createdAt")));
            model.put("createdIso", isoDate(text("createdAt")));

            // 연령 제한은 색인 제외. follow 는 유지
            model.put("robots", adult ? "noindex, follow" : "index, follow");

            return model;
        }

        private String text(String key) {
            Object value = clip.get(key);
            return (value == null) ? "" : value.toString();
        }

        /** 조회수·길이는 문자열. 숫자 아니면 0 */
        private long number(String key) {
            try {
                String value = text(key);
                return value.isEmpty() ? 0L : Long.parseLong(value.trim());
            } catch (NumberFormatException e) {
                return 0L;
            }
        }

        /** https 만 허용 */
        private static String safeImageUrl(String url) {
            return url.startsWith("https://") ? url : "";
        }

        private static String description(String title, long views) {
            return "이리온(IRION) 클립 \"" + title + "\" — 조회수 " + comma(views)
                    + "회. 치지직 원본으로 이어집니다.";
        }

        static String comma(long value) {
            return String.format("%,d", value);
        }

        /** "0:15", 한 시간 초과 시 "1:02:03" */
        static String duration(long seconds) {
            if (seconds <= 0) {
                return "0:00";
            }
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            long rest = seconds % 60;

            if (hours > 0) {
                return String.format("%d:%02d:%02d", hours, minutes, rest);
            }
            return String.format("%d:%02d", minutes, rest);
        }

        /** schema.org duration 은 ISO 8601 — "PT15S" */
        static String isoDuration(long seconds) {
            if (seconds <= 0) {
                return "PT0S";
            }
            StringBuilder out = new StringBuilder("PT");
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            long rest = seconds % 60;

            if (hours > 0)   out.append(hours).append('H');
            if (minutes > 0) out.append(minutes).append('M');
            if (rest > 0)    out.append(rest).append('S');
            return out.toString();
        }

        /** "2025-07-30 21:54:50" → "2025년 7월 30일". 형식 불일치 시 빈 문자열 */
        static String koreanDate(String createdAt) {
            if (createdAt.length() < 10) {
                return "";
            }
            try {
                int year = Integer.parseInt(createdAt.substring(0, 4));
                int month = Integer.parseInt(createdAt.substring(5, 7));
                int day = Integer.parseInt(createdAt.substring(8, 10));
                return year + "년 " + month + "월 " + day + "일";
            } catch (NumberFormatException e) {
                return "";
            }
        }

        /** "2025-07-30 21:54:50" → "2025-07-30T21:54:50+09:00". 치지직 시각은 KST */
        static String isoDate(String createdAt) {
            if (createdAt.length() < 19) {
                return "";
            }
            return createdAt.substring(0, 10) + "T" + createdAt.substring(11, 19) + "+09:00";
        }
    }
}

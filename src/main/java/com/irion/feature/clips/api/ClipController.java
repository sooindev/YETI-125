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
 * 클립 아카이브 화면.
 *
 * 목록은 홈과 같은 방식으로 브라우저가 그리지만(/live/clips), <b>상세는 서버가 그린다</b>.
 * 이 화면이 존재하는 이유가 "클립 하나가 자기 주소를 갖는 것"이라서다 — 제목과 썸네일이
 * HTML 에 박혀 나가야 검색엔진이 색인하고 카카오톡·X 가 미리보기를 뽑는다.
 * 자바스크립트가 채우는 화면은 그 둘 다 놓친다.
 *
 * 재생은 여기서 하지 않는다. 치지직 임베드를 쓰지 않고 원본으로 보낸다 —
 * 다시보기가 이미 그렇게 동작하고, 이 화면도 같은 규칙을 따른다.
 */
@Controller
@RequestMapping("/clips")
public class ClipController {

    /**
     * 치지직 clipUID 의 모양. 주소에서 받은 값을 화면에 그대로 싣기 전에 여기서 한 번 거른다.
     * 여기 걸리지 않는 주소는 아무 핸들러에도 닿지 않아 404 가 된다.
     */
    private static final String CLIP_ID = "[A-Za-z0-9_-]{1,64}";

    /** 썸네일이 없을 때 자리를 채우는 투명 1x1. src 를 비우면 브라우저가 현재 페이지를 이미지로 받아 온다 */
    private static final String BLANK_PIXEL =
            "data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7";

    /** 썸네일이 없거나 가려진 클립의 공유 미리보기. 빈 카드보다는 사이트 얼굴이 낫다 */
    private static final String FALLBACK_OG_IMAGE =
            Site.url("/resources/images/profile/Irion-profile.jpg");

    @Autowired
    private LiveFeedService liveFeed;

    @GetMapping("")
    public String list() {
        return "pages/clips";
    }

    /** 목록을 아직 덜 받았을 때 다시 오라고 이르는 시간(초) */
    private static final String RETRY_AFTER = "30";

    /**
     * 클립 한 건.
     *
     * 없는 클립 · 지워진 클립 · 남의 채널 클립이 404 로 모인다.
     * findClip 이 우리 채널 목록에 있는지로 가리므로, 이 주소로 남의 클립이 열리지 않는다.
     *
     * <b>못 찾았다고 늘 404 는 아니다.</b> 목록을 아직 덜 받은 순간(캐시가 막 만료돼
     * 다시 채우는 중이거나, 치지직이 죽었을 때)에도 못 찾는다. 그때 404 를 내면
     * 멀쩡한 주소를 검색엔진이 "사라졌다" 로 읽고 색인에서 지운다 — 되돌리는 데 몇 주가 걸린다.
     * 그래서 그 경우는 503 으로, 잠시 뒤 다시 오라고 답한다.
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
     * 화면이 그대로 찍어도 되는 모양으로 다듬은 클립 하나.
     *
     * 이스케이프를 화면이 아니라 여기서 하는 것은, JSP 에 JSTL 이 없어 ${...} 가 받은 글자를
     * 그대로 내보내기 때문이다. 클립 제목은 클립을 딴 시청자가 붙인 이름이라 우리가 쓴 글이 아니다.
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

            // 썸네일이 없으면 img 를 숨기고 대체 자리를 켠다. 홈 카드(media.js)와 같은 모양이다
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

            /*
             * 연령 제한 클립은 색인에서 뺀다. 막을 방법이 우리에게 없는 화면을
             * 검색 결과로 데려오지 않겠다는 뜻이고, follow 는 남겨 목록으로는 이어지게 둔다.
             */
            model.put("robots", adult ? "noindex, follow" : "index, follow");

            return model;
        }

        private String text(String key) {
            Object value = clip.get(key);
            return (value == null) ? "" : value.toString();
        }

        /** 치지직이 주는 조회수·길이는 문자열이다. 숫자가 아니면 0 으로 본다 */
        private long number(String key) {
            try {
                String value = text(key);
                return value.isEmpty() ? 0L : Long.parseLong(value.trim());
            } catch (NumberFormatException e) {
                return 0L;
            }
        }

        /** 화면과 og:image 에 실어도 되는 주소인가. 스킴이 https 가 아니면 버린다 */
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

        /** "0:15" · 한 시간을 넘기면 "1:02:03" */
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

        /** schema.org 의 duration 은 ISO 8601 이라야 한다 — "PT15S" */
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

        /**
         * "2025-07-30 21:54:50" → "2025년 7월 30일".
         * 치지직이 주는 모양이 바뀌면 빈 문자열이 되어 화면에서 날짜 줄만 비고, 나머지는 그대로 선다.
         */
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

        /** "2025-07-30 21:54:50" → "2025-07-30T21:54:50+09:00". 치지직 시각은 KST 다 */
        static String isoDate(String createdAt) {
            if (createdAt.length() < 19) {
                return "";
            }
            return createdAt.substring(0, 10) + "T" + createdAt.substring(11, 19) + "+09:00";
        }
    }
}

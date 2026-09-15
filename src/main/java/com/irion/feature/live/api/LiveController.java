package com.irion.feature.live.api;

import com.irion.integration.chzzk.LiveFeedService;
import com.irion.common.api.JsonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 치지직 연동 조회 API. 파라미터를 다듬고 응답 모양만 만든다. */
@Controller
@RequestMapping("/live")
public class LiveController {

    /** 상한이 없으면 ?limit=500 하나로 외부 API 를 여러 번 부르게 만들 수 있다 */
    private static final int MAX_LIMIT = 50;

    /** 최신순. 그 밖의 값은 전부 인기순(치지직이 주는 차례)으로 본다 */
    static final String SORT_LATEST = "latest";

    /** 검색어 길이 상한. 제목보다 긴 말은 어차피 아무것도 맞히지 못한다 */
    private static final int MAX_QUERY = 50;

    @Autowired
    private LiveFeedService liveFeed;

    @GetMapping("/status")
    @ResponseBody
    public JsonResult getLiveStatus() {
        Map<String, Object> data = liveFeed.getLiveStatus();

        if (data == null) {
            return JsonResult.fail("방송 상태 확인 중 오류 발생");
        }
        return JsonResult.success("조회 성공", data);
    }

    /**
     * 인기 클립 목록.
     *
     * 정렬도 검색도 없으면 홈이 쓰는 "필요한 만큼만 이어 받는" 경로를 탄다.
     * 최신순이나 검색어가 붙으면 그럴 수 없다 — 앞 몇 장만 놓고 고르면 답이 틀린다.
     * 그때는 전량을 받아 거른다 (LiveFeedService.getAllClips).
     */
    @GetMapping("/clips")
    @ResponseBody
    public JsonResult getClips(@RequestParam(defaultValue = "6") int limit,
                               @RequestParam(defaultValue = "0") int offset,
                               @RequestParam(required = false) String sort,
                               @RequestParam(required = false) String q) {

        int safeLimit = clampLimit(limit);
        int safeOffset = Math.max(0, offset);
        String query = clampQuery(q);
        boolean wholeList = SORT_LATEST.equals(sort) || !query.isEmpty();

        if (wholeList) {
            LiveFeedService.ClipFeed feed = liveFeed.getAllClips();
            if (feed == null) {
                return JsonResult.fail("클립 조회 중 오류 발생");
            }

            List<Map<String, Object>> arranged = arrange(feed.getClips(), sort, query);
            Map<String, Object> result = paginate(arranged, "clips", safeOffset, safeLimit);
            // 전량을 놓고 자른 것이라 paginate 의 hasMore 가 그대로 맞다
            result.put("total", arranged.size());

            /*
             * 아직 다 못 받았다는 것을 알린다.
             *
             * 캐시를 다시 채우는 몇 초 동안 다른 스레드는 있는 만큼만 받아 간다.
             * 그 목록으로 최신순을 매기면 조용히, 그리고 체계적으로 틀린다 —
             * 새 클립일수록 조회수가 낮아 인기순 목록의 뒤쪽에 있어서, 덜 받은 목록에는
             * 정작 최신 클립이 빠져 있다. 화면이 잠시 뒤 다시 물어보게 한다.
             */
            if (feed.canGrow()) {
                result.put("partial", true);
            }
            return JsonResult.success("조회 성공", result);
        }

        // 확장 목표도 메모리 상한 안에서
        int need = (int) Math.min((long) safeOffset + safeLimit, LiveFeedService.CLIP_MAX);

        LiveFeedService.ClipFeed feed = liveFeed.getClips(need);

        if (feed == null) {
            return JsonResult.fail("클립 조회 중 오류 발생");
        }

        Map<String, Object> result = paginate(feed.getClips(), "clips", safeOffset, safeLimit);
        // 커서가 남아 있으면 더 있는 것이다
        if (feed.canGrow()) {
            result.put("hasMore", true);
        }
        return JsonResult.success("조회 성공", result);
    }

    @GetMapping("/videos")
    @ResponseBody
    public JsonResult getVideos(@RequestParam(defaultValue = "6") int limit,
                                @RequestParam(defaultValue = "0") int offset) {

        List<Map<String, Object>> all = liveFeed.getVideos();

        if (all == null) {
            return JsonResult.fail("다시보기 조회 중 오류 발생");
        }
        return JsonResult.success("조회 성공",
                paginate(all, "videos", Math.max(0, offset), clampLimit(limit)));
    }

    /**
     * 검색·정렬을 적용한 새 목록. 캐시가 들고 있는 원본은 건드리지 않는다 —
     * 여기서 제자리 정렬하면 다음 요청이 뒤섞인 차례를 인기순이라고 믿는다.
     */
    List<Map<String, Object>> arrange(List<Map<String, Object>> clips, String sort, String query) {
        if (clips == null || clips.isEmpty()) {
            return new ArrayList<Map<String, Object>>();
        }

        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> clip : clips) {
            if (query.isEmpty() || text(clip.get("clipTitle")).toLowerCase(Locale.ROOT).contains(query)) {
                result.add(clip);
            }
        }

        if (SORT_LATEST.equals(sort)) {
            // "2025-07-30 21:54:50" 은 자리수가 고정이라 문자열 비교가 곧 시간 비교다.
            // 날짜를 못 받은 항목은 빈 문자열이 되어 뒤로 밀린다
            Collections.sort(result, new Comparator<Map<String, Object>>() {
                @Override
                public int compare(Map<String, Object> a, Map<String, Object> b) {
                    return text(b.get("createdAt")).compareTo(text(a.get("createdAt")));
                }
            });
        }
        return result;
    }

    /** 검색어를 다듬는다 — 앞뒤 공백을 떼고, 길이를 자르고, 비교는 소문자로 한다 */
    private String clampQuery(String q) {
        if (q == null) {
            return "";
        }
        String trimmed = q.trim();
        if (trimmed.length() > MAX_QUERY) {
            trimmed = trimmed.substring(0, MAX_QUERY);
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    private static String text(Object value) {
        return (value == null) ? "" : value.toString();
    }

    /** limit 을 1..MAX_LIMIT 범위로 가둔다 */
    private int clampLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    /** 경계값을 테스트에서 확인하므로 package-private */
    Map<String, Object> paginate(List<Map<String, Object>> list, String key, int offset, int limit) {
        Map<String, Object> result = new HashMap<String, Object>();

        if (list == null || list.isEmpty()) {
            result.put(key, new ArrayList<Map<String, Object>>());
            result.put("hasMore", false);
            return result;
        }

        // offset 이 목록을 넘으면 subList 가 예외를 던진다
        int start = Math.max(0, Math.min(offset, list.size()));
        int endIndex = Math.min(start + Math.max(0, limit), list.size());
        result.put(key, new ArrayList<Map<String, Object>>(list.subList(start, endIndex)));
        result.put("hasMore", endIndex < list.size());
        result.put("nextOffset", endIndex);

        return result;
    }
}

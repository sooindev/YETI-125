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

/** 치지직 연동 조회 API. 파라미터 정리와 응답 구성만 */
@Controller
@RequestMapping("/live")
public class LiveController {

    /** 상한 없으면 ?limit=500 하나로 외부 API 를 여러 번 호출시킬 수 있음 */
    private static final int MAX_LIMIT = 50;

    /** 최신순. 그 외는 인기순(치지직 기본 순서) */
    static final String SORT_LATEST = "latest";

    /** 검색어 길이 상한 */
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
     * 인기 클립 목록. 정렬·검색 없으면 필요한 만큼만 적재.
     * 최신순·검색은 앞 몇 장으로 답이 틀리므로 전량 적재 후 필터
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
            // 전량 기준이라 paginate 의 hasMore 가 그대로 유효
            result.put("total", arranged.size());

            /*
             * 미완성 목록 표시. 이걸로 최신순을 매기면 체계적으로 틀림 —
             * 새 클립일수록 조회수가 낮아 뒤쪽에 있어 덜 받은 목록에는 최신이 빠짐
             */
            if (feed.canGrow()) {
                result.put("partial", true);
            }
            return JsonResult.success("조회 성공", result);
        }

        // 확장 목표도 메모리 상한 내로
        int need = (int) Math.min((long) safeOffset + safeLimit, LiveFeedService.CLIP_MAX);

        LiveFeedService.ClipFeed feed = liveFeed.getClips(need);

        if (feed == null) {
            return JsonResult.fail("클립 조회 중 오류 발생");
        }

        Map<String, Object> result = paginate(feed.getClips(), "clips", safeOffset, safeLimit);
        // 커서가 남았으면 더 있음
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
     * 검색·정렬을 적용한 새 목록. 원본 불변 —
     * 제자리 정렬하면 다음 요청이 뒤섞인 순서를 인기순으로 오인
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
            // 자리수 고정이라 문자열 비교 = 시간 비교. 날짜 없으면 뒤로
            Collections.sort(result, new Comparator<Map<String, Object>>() {
                @Override
                public int compare(Map<String, Object> a, Map<String, Object> b) {
                    return text(b.get("createdAt")).compareTo(text(a.get("createdAt")));
                }
            });
        }
        return result;
    }

    /** 공백 제거 · 길이 제한 · 소문자 변환 */
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

    /** limit 을 1..MAX_LIMIT 로 제한 */
    private int clampLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    /** 경계값 테스트 대상이라 package-private */
    Map<String, Object> paginate(List<Map<String, Object>> list, String key, int offset, int limit) {
        Map<String, Object> result = new HashMap<String, Object>();

        if (list == null || list.isEmpty()) {
            result.put(key, new ArrayList<Map<String, Object>>());
            result.put("hasMore", false);
            return result;
        }

        // offset 이 목록을 넘으면 subList 예외
        int start = Math.max(0, Math.min(offset, list.size()));
        int endIndex = Math.min(start + Math.max(0, limit), list.size());
        result.put(key, new ArrayList<Map<String, Object>>(list.subList(start, endIndex)));
        result.put("hasMore", endIndex < list.size());
        result.put("nextOffset", endIndex);

        return result;
    }
}

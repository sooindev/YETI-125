package com.irion.common.controller;

import com.irion.common.service.LiveFeedService;
import com.irion.common.util.JsonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 치지직 연동 조회 API.
 *
 * 외부 호출과 파싱은 ChzzkClient, 캐시는 LiveFeedService 가 맡는다.
 * 여기서는 요청 파라미터를 다듬고 응답 모양을 만드는 일만 한다.
 */
@Controller
@RequestMapping("/live")
public class LiveController {

    /**
     * 한 요청이 가져갈 수 있는 최대 개수.
     *
     * 상한이 없으면 ?limit=500 하나로 매번 목록 확장 경로를 태울 수 있다.
     * 확장은 외부 API 를 여러 번 부르므로, 그 길을 아무나 마음대로
     * 열게 두지 않는다. 화면은 6개씩 받아간다.
     */
    private static final int MAX_LIMIT = 50;

    @Autowired
    private LiveFeedService liveFeed;

    /** 방송 상태 조회 */
    @GetMapping("/status")
    @ResponseBody
    public JsonResult getLiveStatus() {
        Map<String, Object> data = liveFeed.getLiveStatus();

        if (data == null) {
            return JsonResult.fail("방송 상태 확인 중 오류 발생");
        }
        return JsonResult.success("조회 성공", data);
    }

    /** 클립 목록 조회 (인기순) */
    @GetMapping("/clips")
    @ResponseBody
    public JsonResult getClips(@RequestParam(defaultValue = "6") int limit,
                               @RequestParam(defaultValue = "0") int offset) {

        int safeLimit = clampLimit(limit);
        int safeOffset = Math.max(0, offset);

        // 확장 목표도 메모리 상한을 넘기지 않는다
        int need = (int) Math.min((long) safeOffset + safeLimit, LiveFeedService.CLIP_MAX);

        LiveFeedService.ClipFeed feed = liveFeed.getClips(need);

        if (feed == null) {
            return JsonResult.fail("클립 조회 중 오류 발생");
        }

        Map<String, Object> result = paginate(feed.getClips(), "clips", safeOffset, safeLimit);
        // 아직 커서가 남아 있으면 지금 다 보여줬어도 더 있는 것이다
        if (feed.canGrow()) {
            result.put("hasMore", true);
        }
        return JsonResult.success("조회 성공", result);
    }

    /** 다시보기 목록 조회 */
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

    /** limit 을 1..MAX_LIMIT 범위로 가둔다 */
    private int clampLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    /** 페이지네이션 결과 생성 (경계값을 테스트에서 확인하므로 package-private) */
    Map<String, Object> paginate(List<Map<String, Object>> list, String key, int offset, int limit) {
        Map<String, Object> result = new HashMap<String, Object>();

        if (list == null || list.isEmpty()) {
            result.put(key, new ArrayList<Map<String, Object>>());
            result.put("hasMore", false);
            return result;
        }

        // offset 이 목록을 넘으면 subList 가 예외를 던진다. 빈 페이지로 받는다.
        int start = Math.max(0, Math.min(offset, list.size()));
        int endIndex = Math.min(start + Math.max(0, limit), list.size());
        result.put(key, new ArrayList<Map<String, Object>>(list.subList(start, endIndex)));
        result.put("hasMore", endIndex < list.size());
        result.put("nextOffset", endIndex);

        return result;
    }
}

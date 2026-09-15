package com.irion.integration.chzzk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 테스트용 ClipFeed 조립기.
 * 생성자가 패키지 전용이라(캐시 밖에서 만들면 커서가 어긋난다) 다른 패키지 테스트용 창구
 */
public final class ClipFeeds {

    private ClipFeeds() {
    }

    public static LiveFeedService.ClipFeed of(List<Map<String, Object>> clips) {
        return new LiveFeedService.ClipFeed(clips, null, null);
    }

    /** 더 받을 것이 남은 목록 — canGrow() 참 */
    public static LiveFeedService.ClipFeed growable(List<Map<String, Object>> clips) {
        return new LiveFeedService.ClipFeed(clips, "cursor-남음", "100");
    }

    public static LiveFeedService.ClipFeed of(Map<String, Object>... clips) {
        return of(new ArrayList<Map<String, Object>>(Arrays.asList(clips)));
    }

    /** 최소 구성의 클립 한 건 */
    public static Map<String, Object> clip(String id, String title, String createdAt, boolean adult) {
        Map<String, Object> clip = new HashMap<String, Object>();
        clip.put("clipId", id);
        clip.put("clipTitle", title);
        clip.put("createdAt", createdAt);
        clip.put("adult", adult);
        return clip;
    }
}

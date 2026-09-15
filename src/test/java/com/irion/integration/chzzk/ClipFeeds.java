package com.irion.integration.chzzk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 테스트용 ClipFeed 조립기.
 *
 * ClipFeed 의 생성자는 패키지 전용이다 — 캐시 밖에서 아무나 만들면 커서가 어긋나기 때문이다.
 * 다른 패키지의 테스트(사이트맵이 그렇다)가 클립 목록을 흉내 내야 해서 이 창구만 열어 둔다.
 */
public final class ClipFeeds {

    private ClipFeeds() {
    }

    public static LiveFeedService.ClipFeed of(List<Map<String, Object>> clips) {
        return new LiveFeedService.ClipFeed(clips, null, null);
    }

    /** 아직 이어 받을 것이 남은 목록 — canGrow() 가 참이다 */
    public static LiveFeedService.ClipFeed growable(List<Map<String, Object>> clips) {
        return new LiveFeedService.ClipFeed(clips, "cursor-남음", "100");
    }

    public static LiveFeedService.ClipFeed of(Map<String, Object>... clips) {
        return of(new ArrayList<Map<String, Object>>(Arrays.asList(clips)));
    }

    /** 사이트맵·목록 테스트가 쓰는 최소한의 클립 한 건 */
    public static Map<String, Object> clip(String id, String title, String createdAt, boolean adult) {
        Map<String, Object> clip = new HashMap<String, Object>();
        clip.put("clipId", id);
        clip.put("clipTitle", title);
        clip.put("createdAt", createdAt);
        clip.put("adult", adult);
        return clip;
    }
}

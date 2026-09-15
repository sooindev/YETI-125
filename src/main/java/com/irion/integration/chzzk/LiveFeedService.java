package com.irion.integration.chzzk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/** 치지직 목록 캐시. 만료 시 한 스레드만 갱신 */
@Service
public class LiveFeedService {

    private static final Logger logger = LoggerFactory.getLogger(LiveFeedService.class);

    private static final long CACHE_DURATION = 10 * 60 * 1000; // 10분 (클립/비디오)
    private static final long LIVE_CACHE_DURATION = 1 * 60 * 1000; // 1분 (방송 상태)

    // 실패 후 재시도 간격. 없으면 요청마다 5초 타임아웃 재대기.
    // 가장 짧은 TTL(1분)보다 짧게 — 복구 감지가 늦어지면 안 됨
    private static final long FAILURE_BACKOFF = 30 * 1000; // 30초

    private static final int CLIP_INITIAL_PAGES = 2;   // 첫 요청에 미리 담아둘 분량
    public static final int CLIP_MAX = 3000;           // 메모리 상한
    private static final int CLIP_PAGES_PER_REQUEST = 10; // 한 요청이 외부에 낼 수 있는 최대 호출

    // 상한까지 채우는 확장 횟수. extendClips 는 한 번에 10장까지
    private static final int FULL_LOAD_ROUNDS =
            (CLIP_MAX + CLIP_PAGES_PER_REQUEST * ChzzkClient.CLIP_PAGE_SIZE - 1)
                    / (CLIP_PAGES_PER_REQUEST * ChzzkClient.CLIP_PAGE_SIZE);

    // 다시보기 페이지 상한 — 쌓이면 한 페이지를 넘음
    private static final int VIDEO_MAX_PAGES = 20;

    /** 이 사이트에서만 감출 다시보기. 제목은 바뀌므로 videoNo 기준 */
    private static final Set<String> HIDDEN_VIDEO_NOS = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList(
                    "319019" // 이리온의 재채기.mp4 (2024-02-29)
            )));

    @Autowired
    private ChzzkClient chzzk;

    // 싱글턴 — 참조만 교체, 갱신은 락으로 한 스레드만
    private final AtomicReference<Snapshot<Map<String, Object>>> liveStatusCache = new AtomicReference<>();
    private final AtomicReference<Snapshot<ClipFeed>> clipsCache = new AtomicReference<>();
    private final AtomicReference<Snapshot<List<Map<String, Object>>>> videosCache = new AtomicReference<>();

    private final Object liveStatusLock = new Object();
    private final Object clipsLock = new Object();
    private final Object videosLock = new Object();

    /** 확장 진행 중 여부. 확장은 락 밖에서, 겹치면 있는 만큼만 */
    private final AtomicBoolean clipsExtending = new AtomicBoolean(false);

    /** 클립 조각과 다음 커서. chzzk 페이징은 offset 이 아니라 커서 */
    public static final class ClipFeed {
        private final List<Map<String, Object>> clips;
        private final String nextClipUID;
        private final String nextReadCount;

        /** clipId → 클립. 상세 화면의 단건 조회용 */
        private final Map<String, Map<String, Object>> byId;

        ClipFeed(List<Map<String, Object>> clips, String nextClipUID, String nextReadCount) {
            this.clips = clips;
            this.nextClipUID = nextClipUID;
            this.nextReadCount = nextReadCount;

            Map<String, Map<String, Object>> index =
                    new HashMap<String, Map<String, Object>>(Math.max(16, clips.size() * 2));
            for (Map<String, Object> clip : clips) {
                Object id = clip.get("clipId");
                if (id instanceof String) {
                    index.put((String) id, clip);
                }
            }
            this.byId = index;
        }

        public List<Map<String, Object>> getClips() {
            return clips;
        }

        /** 목록에 있으면 클립, 없으면 null */
        public Map<String, Object> get(String clipId) {
            return (clipId == null) ? null : byId.get(clipId);
        }

        public int size() {
            return clips.size();
        }

        boolean hasNext() {
            return nextClipUID != null && !nextClipUID.isEmpty();
        }

        /** 커서 남음 + 상한 미도달 */
        public boolean canGrow() {
            return hasNext() && clips.size() < CLIP_MAX;
        }
    }

    /** 방송 상태. 실패 시 null */
    public Map<String, Object> getLiveStatus() {
        return cached("방송 상태", liveStatusCache, liveStatusLock, LIVE_CACHE_DURATION, chzzk::fetchLiveStatus);
    }

    /** 클립을 need 개까지. 실패 시 null */
    public ClipFeed getClips(int need) {
        ClipFeed feed = cached("클립", clipsCache, clipsLock, CACHE_DURATION, this::loadClips);
        return (feed == null) ? null : extendClips(feed, need);
    }

    /**
     * 클립 조회 결과 — "없음" 과 "아직 모름" 의 구분.
     * 구분하지 않으면 목록을 덜 받은 순간의 멀쩡한 주소가 404 → 색인 삭제
     */
    public static final class ClipLookup {
        private final Map<String, Object> clip;
        private final boolean complete;

        ClipLookup(Map<String, Object> clip, boolean complete) {
            this.clip = clip;
            this.complete = complete;
        }

        /** 찾은 클립, 없으면 null */
        public Map<String, Object> getClip() {
            return clip;
        }

        /**
         * 목록을 끝까지 받았는지. 거짓이면 "없음" 으로 단정 불가.
         * 상한(CLIP_MAX) 도달도 참 — 상한을 넘는 클립은 오래된 것부터 조회 불가
         */
        public boolean isComplete() {
            return complete;
        }
    }

    /**
     * 클립 한 건 + 우리 채널 소유 확인.
     * 치지직 단건 API 는 채널을 주지 않아, 우리 목록에 있는지가 유일한 판단 근거
     */
    public ClipLookup findClip(String clipId) {
        if (clipId == null || clipId.isEmpty()) {
            // 아이디 모양 아님 — 더 받아도 없음
            return new ClipLookup(null, true);
        }

        ClipFeed feed = fullClips();
        if (feed == null) {
            // 목록 자체가 없음 — 클립이 없다는 뜻은 아님
            return new ClipLookup(null, false);
        }

        return new ClipLookup(feed.get(clipId), !feed.canGrow());
    }

    /** 클립 전량. 최신순 · 검색 · 사이트맵용. 실패 시 null */
    public ClipFeed getAllClips() {
        return fullClips();
    }

    /**
     * 커서가 마를 때까지 적재. 34페이지에 3초쯤, 이후 10분은 캐시.
     * 다른 스레드가 확장 중이면 크기 그대로 반환 — 대기 없이 있는 만큼만
     */
    private ClipFeed fullClips() {
        ClipFeed feed = getClips(CLIP_MAX);

        for (int round = 0; round < FULL_LOAD_ROUNDS && feed != null && feed.canGrow(); round++) {
            int before = feed.size();
            feed = getClips(CLIP_MAX);
            if (feed == null || feed.size() == before) {
                break;
            }
        }
        return feed;
    }

    /** 다시보기 목록. 실패 시 null */
    public List<Map<String, Object>> getVideos() {
        return cached("다시보기", videosCache, videosLock, CACHE_DURATION, this::loadVideos);
    }

    /** 인기 클립 첫 묶음 */
    private ClipFeed loadClips() {
        ClipFeed feed = new ClipFeed(new ArrayList<Map<String, Object>>(), null, null);
        feed = fetchMoreClips(feed, CLIP_INITIAL_PAGES);

        // 빈 목록 캐시 금지 — 장애가 10분간 굳음
        return feed.clips.isEmpty() ? null : feed;
    }

    /** need 개까지 확장. 적재 시각 갱신으로 더보기 중 TTL 만료 방지 */
    private ClipFeed extendClips(ClipFeed feed, int need) {
        if (feed.clips.size() >= need || !feed.hasNext() || feed.clips.size() >= CLIP_MAX) {
            return feed;
        }

        // 시작 지점만 락 안에서 결정
        ClipFeed base;
        synchronized (clipsLock) {
            Snapshot<ClipFeed> snapshot = clipsCache.get();
            base = (snapshot != null && snapshot.value != null) ? snapshot.value : feed;

            if (base.clips.size() >= need || !base.hasNext()) {
                return base;
            }
            // 이미 확장 중이면 대기하지 않음
            if (!clipsExtending.compareAndSet(false, true)) {
                return base;
            }
        }

        int shortfall = need - base.clips.size();
        int pages = Math.min((shortfall + ChzzkClient.CLIP_PAGE_SIZE - 1) / ChzzkClient.CLIP_PAGE_SIZE,
                CLIP_PAGES_PER_REQUEST);

        try {
            // 외부 호출은 락 밖에서 — 가장 오래 걸리는 구간
            ClipFeed grown = fetchMoreClips(base, pages);

            synchronized (clipsLock) {
                Snapshot<ClipFeed> snapshot = clipsCache.get();
                ClipFeed current = (snapshot != null) ? snapshot.value : null;

                // 받는 사이 목록이 교체됐으면 우리 커서는 지난 세대
                if (current != null && current != base) {
                    return current;
                }

                // 성공 — 실패 기록도 제거
                clipsCache.set(new Snapshot<ClipFeed>(grown, System.currentTimeMillis(), 0L));
                return grown;
            }
        } finally {
            clipsExtending.set(false);
        }
    }

    /** 커서를 따라 pages 만큼 적재 후 뒤에 추가 */
    private ClipFeed fetchMoreClips(ClipFeed feed, int pages) {
        List<Map<String, Object>> clips = new ArrayList<Map<String, Object>>(feed.clips);

        Set<String> ids = new HashSet<String>();
        for (Map<String, Object> clip : clips) {
            ids.add((String) clip.get("clipId"));
        }

        String uid = feed.nextClipUID;
        String readCount = feed.nextReadCount;
        boolean first = clips.isEmpty();

        for (int page = 0; page < pages && clips.size() < CLIP_MAX; page++) {
            if (!first && (uid == null || uid.isEmpty()))
                break;

            ChzzkClient.ClipPage fetched = chzzk.fetchClipPage(uid, readCount);
            if (fetched == null)
                break;

            int before = clips.size();
            for (Map<String, Object> clip : fetched.getClips()) {
                String id = (String) clip.get("clipId");
                if (id != null && ids.add(id)) {
                    clips.add(clip);
                }
            }

            uid = fetched.getNextClipUID();
            readCount = fetched.getNextReadCount();
            first = false;

            // 같은 페이지 반복 시 무한 루프 방지
            if (clips.size() == before)
                break;
        }

        if (clips.size() > CLIP_MAX)
            clips = new ArrayList<Map<String, Object>>(clips.subList(0, CLIP_MAX));

        return new ClipFeed(clips, uid, readCount);
    }

    /** 다시보기 적재. 빈 페이지가 나올 때까지 */
    private List<Map<String, Object>> loadVideos() {
        List<Map<String, Object>> videos = new ArrayList<Map<String, Object>>();
        Set<String> ids = new HashSet<String>();

        for (int page = 0; page < VIDEO_MAX_PAGES; page++) {
            List<Map<String, Object>> batch = chzzk.fetchVideoPage(page);
            if (batch == null || batch.isEmpty())
                break;

            for (Map<String, Object> video : batch) {
                String no = (String) video.get("videoNo");
                if (no == null || HIDDEN_VIDEO_NOS.contains(no))
                    continue;
                if (ids.add(no)) {
                    videos.add(video);
                }
            }

            // 마지막 페이지
            if (batch.size() < ChzzkClient.VIDEO_PAGE_SIZE)
                break;
        }

        // 첫 페이지부터 실패 시 만료 캐시로 폴백
        return videos.isEmpty() ? null : videos;
    }

    /** 값 · 적재 시각 · 마지막 실패 시각. 따로 두면 "새 값 + 옛 시각" 조합이 생김 */
    private static final class Snapshot<T> {
        final T value;
        final long loadedAt;

        /** 마지막 실패 시각. 없으면 0 */
        final long failedAt;

        Snapshot(T value, long loadedAt, long failedAt) {
            this.value = value;
            this.loadedAt = loadedAt;
            this.failedAt = failedAt;
        }

        /** 실패 기록은 값 없이도 남으므로 value 도 확인 */
        boolean isFresh(long ttl) {
            return value != null && System.currentTimeMillis() - loadedAt <= ttl;
        }

        /** 백오프 중 — 재호출 금지 */
        boolean inBackoff() {
            return failedAt != 0 && System.currentTimeMillis() - failedAt < FAILURE_BACKOFF;
        }
    }

    /**
     * 만료 시 갱신. 한 스레드만 진입, 실패하면 만료값이라도 반환.
     * 락 안에서도 백오프 확인 — 줄 서 있던 스레드가 첫 실패를 보고 물러나야 함
     */
    private <T> T cached(String name, AtomicReference<Snapshot<T>> ref, Object lock,
                         long ttl, Supplier<T> loader) {

        Snapshot<T> snapshot = ref.get();
        if (snapshot != null && (snapshot.isFresh(ttl) || snapshot.inBackoff())) {
            return snapshot.value;
        }

        synchronized (lock) {
            // 대기 중 다른 스레드가 갱신했거나 실패했을 수 있음
            snapshot = ref.get();
            if (snapshot != null && (snapshot.isFresh(ttl) || snapshot.inBackoff())) {
                return snapshot.value;
            }

            // 상태 전환 시에만 로그를 남기려고 미리 확인
            boolean wasFailing = snapshot != null && snapshot.failedAt != 0;
            String thrown = null;

            try {
                T loaded = loader.get();
                if (loaded != null) {
                    if (wasFailing) {
                        logger.info("치지직 {} 갱신이 다시 됩니다", name);
                    }
                    // 성공 — 실패 기록 제거
                    ref.set(new Snapshot<>(loaded, System.currentTimeMillis(), 0L));
                    return loaded;
                }
            } catch (Exception e) {
                // 여기까지 왔으면 호출이 아니라 파싱이 깨진 것 — 원인을 들고 간다
                thrown = e.getClass().getSimpleName()
                        + (e.getMessage() != null ? ": " + e.getMessage() : "");
            }

            // 값 유지, 실패 시각만 갱신. 버리면 백오프 동안 화면이 빔
            T stale = (snapshot != null) ? snapshot.value : null;
            long staleAt = (snapshot != null) ? snapshot.loadedAt : 0L;

            // 실패 시작 시에만. 매번 남기면 장애 하루에 수천 줄
            if (!wasFailing) {
                logFailureStarted(name, stale, staleAt, thrown);
            }

            ref.set(new Snapshot<>(stale, staleAt, System.currentTimeMillis()));
            return stale;
        }
    }

    /** 무엇이 · 언제부터 · 왜 안 되는지 한 줄로 */
    private void logFailureStarted(String name, Object stale, long staleAt, String thrown) {
        String cause = (thrown != null) ? thrown : "치지직이 값을 주지 않았습니다";

        if (stale == null) {
            logger.warn("치지직 {} 를 아직 한 번도 받지 못했습니다 — {}", name, cause);
            return;
        }

        long minutes = (System.currentTimeMillis() - staleAt) / 60000;
        logger.warn("치지직 {} 갱신 실패 — {}분 전 값으로 버팁니다 ({})", name, minutes, cause);
    }
}

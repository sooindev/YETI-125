package com.irion.common.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * 치지직 목록의 캐시 계층.
 *
 * ChzzkClient 가 가져온 것을 담아두고, 만료되면 한 스레드만 갱신한다.
 * 컨트롤러는 여기서 받은 목록을 잘라 응답으로 만들기만 한다.
 */
@Service
public class LiveFeedService {

    private static final long CACHE_DURATION = 10 * 60 * 1000; // 10분 (클립/비디오)
    private static final long LIVE_CACHE_DURATION = 1 * 60 * 1000; // 1분 (방송 상태)

    private static final int CLIP_INITIAL_PAGES = 2;   // 첫 요청에 미리 담아둘 분량
    public static final int CLIP_MAX = 3000;           // 메모리 상한
    private static final int CLIP_PAGES_PER_REQUEST = 10; // 한 요청이 외부에 낼 수 있는 최대 호출

    // 다시보기 — 현재 18개뿐이지만 쌓이면 한 페이지를 넘는다
    private static final int VIDEO_MAX_PAGES = 20;

    /**
     * 다시보기에서 감출 항목. 제목은 바뀔 수 있으므로 videoNo 로 거른다.
     * 이 사이트에서만 숨기는 것이고 치지직에는 그대로 남아 있다.
     */
    private static final Set<String> HIDDEN_VIDEO_NOS = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList(
                    "319019" // 이리온의 재채기.mp4 (2024-02-29)
            )));

    @Autowired
    private ChzzkClient chzzk;

    // 캐시
    // 이 빈은 싱글턴이라 여러 요청 스레드가 동시에 접근한다.
    // 값과 적재 시각을 스냅샷 하나로 묶어 참조만 교체하고, 갱신은
    // 락으로 한 스레드에만 맡긴다. (아래 cached() 참고)
    private final AtomicReference<Snapshot<Map<String, Object>>> liveStatusCache = new AtomicReference<>();
    private final AtomicReference<Snapshot<ClipFeed>> clipsCache = new AtomicReference<>();
    private final AtomicReference<Snapshot<List<Map<String, Object>>>> videosCache = new AtomicReference<>();

    private final Object liveStatusLock = new Object();
    private final Object clipsLock = new Object();
    private final Object videosLock = new Object();

    /**
     * 지금 클립을 이어 받는 중인가.
     *
     * 확장은 외부 API 를 최대 10회 부르므로 락을 쥔 채로 하면 한 스레드가
     * 수십 초간 락을 잡는다. 락 밖에서 하고, 이미 받아오는 중이면 다른
     * 스레드는 기다리지 않고 지금 있는 만큼만 받아간다.
     */
    private final AtomicBoolean clipsExtending = new AtomicBoolean(false);

    /**
     * 클립 목록 조각과 다음 커서. chzzk 의 클립 페이징은 offset 이 아니라 커서다.
     */
    public static final class ClipFeed {
        private final List<Map<String, Object>> clips;
        private final String nextClipUID;
        private final String nextReadCount;

        ClipFeed(List<Map<String, Object>> clips, String nextClipUID, String nextReadCount) {
            this.clips = clips;
            this.nextClipUID = nextClipUID;
            this.nextReadCount = nextReadCount;
        }

        public List<Map<String, Object>> getClips() {
            return clips;
        }

        boolean hasNext() {
            return nextClipUID != null && !nextClipUID.isEmpty();
        }

        /** 아직 더 받아올 여지가 있는가 — 커서가 남았고 상한에도 닿지 않았다 */
        public boolean canGrow() {
            return hasNext() && clips.size() < CLIP_MAX;
        }
    }

    // ========================================
    // 조회
    // ========================================

    /** 방송 상태. 못 가져오면 null. */
    public Map<String, Object> getLiveStatus() {
        return cached(liveStatusCache, liveStatusLock, LIVE_CACHE_DURATION, chzzk::fetchLiveStatus);
    }

    /**
     * 클립 목록을 need 개까지 채워서 돌려준다. 못 가져오면 null.
     */
    public ClipFeed getClips(int need) {
        ClipFeed feed = cached(clipsCache, clipsLock, CACHE_DURATION, this::loadClips);
        return (feed == null) ? null : extendClips(feed, need);
    }

    /** 다시보기 목록. 못 가져오면 null. */
    public List<Map<String, Object>> getVideos() {
        return cached(videosCache, videosLock, CACHE_DURATION, this::loadVideos);
    }

    // ========================================
    // 적재
    // ========================================

    /** 인기 클립 첫 묶음 로드 */
    private ClipFeed loadClips() {
        ClipFeed feed = new ClipFeed(new ArrayList<Map<String, Object>>(), null, null);
        feed = fetchMoreClips(feed, CLIP_INITIAL_PAGES);

        // 한 건도 못 모았으면 API 장애로 보고 실패를 알린다.
        // (빈 목록을 캐시해 두면 장애가 10분간 굳어버린다)
        return feed.clips.isEmpty() ? null : feed;
    }

    /**
     * 캐시 목록을 need 개까지 늘린다. 늘리는 동안 적재 시각을 새로 찍어,
     * 더보기를 누르는 사이에 TTL 이 끝나 처음부터 다시 쌓이는 일을 막는다.
     */
    private ClipFeed extendClips(ClipFeed feed, int need) {
        if (feed.clips.size() >= need || !feed.hasNext() || feed.clips.size() >= CLIP_MAX) {
            return feed;
        }

        // 어디서부터 이어 받을지만 락 안에서 정한다
        ClipFeed base;
        synchronized (clipsLock) {
            Snapshot<ClipFeed> snapshot = clipsCache.get();
            base = (snapshot != null && snapshot.value != null) ? snapshot.value : feed;

            if (base.clips.size() >= need || !base.hasNext()) {
                return base;
            }
            // 이미 누가 받아오는 중이면 기다리지 않는다
            if (!clipsExtending.compareAndSet(false, true)) {
                return base;
            }
        }

        int shortfall = need - base.clips.size();
        int pages = Math.min((shortfall + ChzzkClient.CLIP_PAGE_SIZE - 1) / ChzzkClient.CLIP_PAGE_SIZE,
                CLIP_PAGES_PER_REQUEST);

        try {
            // 외부 호출은 락 밖에서 — 여기가 오래 걸리는 구간이다
            ClipFeed grown = fetchMoreClips(base, pages);

            synchronized (clipsLock) {
                Snapshot<ClipFeed> snapshot = clipsCache.get();
                ClipFeed current = (snapshot != null) ? snapshot.value : null;

                // 받아오는 사이 다른 스레드가 목록을 다시 쌓았으면 우리 커서는
                // 지난 세대라 이어 붙일 수 없다. 새 것을 두고 우리 결과는 버린다
                if (current != null && current != base) {
                    return current;
                }

                clipsCache.set(new Snapshot<ClipFeed>(grown, System.currentTimeMillis()));
                return grown;
            }
        } finally {
            clipsExtending.set(false);
        }
    }

    /** 커서를 따라 pages 만큼 이어 받아 뒤에 붙인다 */
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

            // 커서가 돌지 않아 같은 페이지가 또 오면 무한 루프가 된다
            if (clips.size() == before)
                break;
        }

        if (clips.size() > CLIP_MAX)
            clips = new ArrayList<Map<String, Object>>(clips.subList(0, CLIP_MAX));

        return new ClipFeed(clips, uid, readCount);
    }

    /**
     * 다시보기 로드. 한 페이지(50개)에 맞춰두면 쌓였을 때 조용히 잘리므로
     * 페이지가 빌 때까지 이어 받는다.
     */
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

        // 첫 페이지부터 실패했으면 만료된 캐시로 폴백시킨다
        return videos.isEmpty() ? null : videos;
    }

    // ========================================
    // 캐시
    // ========================================

    /**
     * 값과 적재 시각을 함께 담는 불변 스냅샷. 각각 필드로 두면
     * "새 값 + 옛 시각" 같은 어긋난 조합이 잠깐 보인다.
     */
    private static final class Snapshot<T> {
        final T value;
        final long loadedAt;

        Snapshot(T value, long loadedAt) {
            this.value = value;
            this.loadedAt = loadedAt;
        }

        boolean isFresh(long ttl) {
            return System.currentTimeMillis() - loadedAt <= ttl;
        }
    }

    /**
     * 캐시에서 읽되 만료됐으면 갱신한다.
     *
     * 갱신은 락으로 묶어 한 스레드만 수행하므로, 만료 순간 요청이 몰려도
     * 외부 API 는 한 번만 호출된다. 갱신에 실패하면 만료된 값이라도 돌려준다
     * — 외부 API 가 흔들려도 화면이 비지 않게. 값이 아예 없을 때만 null.
     */
    private <T> T cached(AtomicReference<Snapshot<T>> ref, Object lock,
                         long ttl, Supplier<T> loader) {

        Snapshot<T> snapshot = ref.get();
        if (snapshot != null && snapshot.isFresh(ttl)) {
            return snapshot.value;
        }

        synchronized (lock) {
            // 락을 기다리는 동안 다른 스레드가 이미 갱신했을 수 있다
            snapshot = ref.get();
            if (snapshot != null && snapshot.isFresh(ttl)) {
                return snapshot.value;
            }

            try {
                T loaded = loader.get();
                if (loaded != null) {
                    ref.set(new Snapshot<>(loaded, System.currentTimeMillis()));
                    return loaded;
                }
            } catch (Exception ignored) {
                // 아래에서 만료된 값으로 폴백한다
            }

            return snapshot != null ? snapshot.value : null;
        }
    }
}

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

/** 치지직 목록 캐시. 만료되면 한 스레드만 갱신한다. */
@Service
public class LiveFeedService {

    private static final long CACHE_DURATION = 10 * 60 * 1000; // 10분 (클립/비디오)
    private static final long LIVE_CACHE_DURATION = 1 * 60 * 1000; // 1분 (방송 상태)

    /**
     * 갱신에 실패한 뒤 다시 두드리기까지. 치지직이 죽으면 호출 하나가 5초(READ_TIMEOUT)를
     * 통째로 쓰므로, 이 시간이 없으면 요청마다 그 5초를 처음부터 다시 기다린다.
     *
     * 가장 짧은 TTL(방송 상태 1분)보다 짧게 둔다 — 복구를 알아채는 데 걸리는 시간이
     * 정상일 때의 갱신 주기보다 늦어지면 안 된다.
     */
    private static final long FAILURE_BACKOFF = 30 * 1000; // 30초

    private static final int CLIP_INITIAL_PAGES = 2;   // 첫 요청에 미리 담아둘 분량
    public static final int CLIP_MAX = 3000;           // 메모리 상한
    private static final int CLIP_PAGES_PER_REQUEST = 10; // 한 요청이 외부에 낼 수 있는 최대 호출

    // 다시보기 — 현재 18개뿐이지만 쌓이면 한 페이지를 넘는다
    private static final int VIDEO_MAX_PAGES = 20;

    /** 이 사이트에서만 감출 다시보기. 제목은 바뀌므로 videoNo 로 거른다. */
    private static final Set<String> HIDDEN_VIDEO_NOS = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList(
                    "319019" // 이리온의 재채기.mp4 (2024-02-29)
            )));

    @Autowired
    private ChzzkClient chzzk;

    // 싱글턴이라 여러 스레드가 함께 쓴다. 참조만 교체하고 갱신은 락으로 한 스레드에만 맡긴다.
    private final AtomicReference<Snapshot<Map<String, Object>>> liveStatusCache = new AtomicReference<>();
    private final AtomicReference<Snapshot<ClipFeed>> clipsCache = new AtomicReference<>();
    private final AtomicReference<Snapshot<List<Map<String, Object>>>> videosCache = new AtomicReference<>();

    private final Object liveStatusLock = new Object();
    private final Object clipsLock = new Object();
    private final Object videosLock = new Object();

    /** 클립을 이어 받는 중인가. 확장은 락 밖에서 하고, 겹치면 있는 만큼만 준다. */
    private final AtomicBoolean clipsExtending = new AtomicBoolean(false);

    /** 클립 조각과 다음 커서. chzzk 의 클립 페이징은 offset 이 아니라 커서다. */
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

        /** 커서가 남았고 상한에도 닿지 않았는가 */
        public boolean canGrow() {
            return hasNext() && clips.size() < CLIP_MAX;
        }
    }

    /** 방송 상태. 못 가져오면 null. */
    public Map<String, Object> getLiveStatus() {
        return cached(liveStatusCache, liveStatusLock, LIVE_CACHE_DURATION, chzzk::fetchLiveStatus);
    }

    /** 클립을 need 개까지 채워서 돌려준다. 못 가져오면 null. */
    public ClipFeed getClips(int need) {
        ClipFeed feed = cached(clipsCache, clipsLock, CACHE_DURATION, this::loadClips);
        return (feed == null) ? null : extendClips(feed, need);
    }

    /** 다시보기 목록. 못 가져오면 null. */
    public List<Map<String, Object>> getVideos() {
        return cached(videosCache, videosLock, CACHE_DURATION, this::loadVideos);
    }

    /** 인기 클립 첫 묶음 로드 */
    private ClipFeed loadClips() {
        ClipFeed feed = new ClipFeed(new ArrayList<Map<String, Object>>(), null, null);
        feed = fetchMoreClips(feed, CLIP_INITIAL_PAGES);

        // 빈 목록을 캐시하면 장애가 10분간 굳는다
        return feed.clips.isEmpty() ? null : feed;
    }

    /** need 개까지 늘린다. 적재 시각을 새로 찍어 더보기 도중 TTL 만료를 막는다. */
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

                // 받는 사이 목록이 다시 쌓였으면 우리 커서는 지난 세대다
                if (current != null && current != base) {
                    return current;
                }

                // 이어 받는 데 성공했으니 실패 기록도 지운다
                clipsCache.set(new Snapshot<ClipFeed>(grown, System.currentTimeMillis(), 0L));
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

    /** 다시보기 로드. 한 페이지에 맞추면 쌓였을 때 잘리므로 빌 때까지 이어 받는다. */
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

    /**
     * 값·적재 시각·마지막 실패 시각을 함께 담는다. 따로 두면 "새 값 + 옛 시각" 조합이 보인다.
     * 실패 시각도 여기 둔다 — 값과 짝이 맞아야 "언제 받은 값을 언제부터 못 갱신하고 있는지"가 하나로 읽힌다.
     */
    private static final class Snapshot<T> {
        final T value;
        final long loadedAt;

        /** 마지막으로 갱신에 실패한 시각. 실패한 적이 없으면 0 */
        final long failedAt;

        Snapshot(T value, long loadedAt, long failedAt) {
            this.value = value;
            this.loadedAt = loadedAt;
            this.failedAt = failedAt;
        }

        /** 실패 기록은 값 없이도 남으므로 value 를 함께 본다 */
        boolean isFresh(long ttl) {
            return value != null && System.currentTimeMillis() - loadedAt <= ttl;
        }

        /** 방금 실패했는가 — 그렇다면 다시 두드리지 않는다 */
        boolean inBackoff() {
            return failedAt != 0 && System.currentTimeMillis() - failedAt < FAILURE_BACKOFF;
        }
    }

    /**
     * 만료면 갱신한다. 갱신은 한 스레드만, 실패하면 만료된 값이라도 돌려준다.
     *
     * 실패를 적어두는 것이 중요하다. 안 적으면 치지직이 죽었을 때 요청마다 락 안에서
     * 5초 타임아웃을 처음부터 다시 기다린다 — 스레드가 줄줄이 밀린다.
     * 락 안에서도 백오프를 보는 이유는, 이미 락 앞에 줄 서 있던 스레드들이
     * 첫 스레드의 실패를 보고 그 자리에서 물러나야 하기 때문이다.
     */
    private <T> T cached(AtomicReference<Snapshot<T>> ref, Object lock,
                         long ttl, Supplier<T> loader) {

        Snapshot<T> snapshot = ref.get();
        if (snapshot != null && (snapshot.isFresh(ttl) || snapshot.inBackoff())) {
            return snapshot.value;
        }

        synchronized (lock) {
            // 기다리는 동안 다른 스레드가 갱신했거나, 갱신에 실패했을 수 있다
            snapshot = ref.get();
            if (snapshot != null && (snapshot.isFresh(ttl) || snapshot.inBackoff())) {
                return snapshot.value;
            }

            try {
                T loaded = loader.get();
                if (loaded != null) {
                    // 성공했으니 실패 기록도 지운다 — 치지직이 돌아왔다
                    ref.set(new Snapshot<>(loaded, System.currentTimeMillis(), 0L));
                    return loaded;
                }
            } catch (Exception ignored) {
                // 아래에서 실패를 적어두고 만료된 값으로 물러난다
            }

            // 값과 적재 시각은 그대로 두고 실패 시각만 새로 찍는다.
            // 값을 버리면 백오프 동안 화면이 빈다.
            T stale = (snapshot != null) ? snapshot.value : null;
            long staleAt = (snapshot != null) ? snapshot.loadedAt : 0L;
            ref.set(new Snapshot<>(stale, staleAt, System.currentTimeMillis()));
            return stale;
        }
    }
}

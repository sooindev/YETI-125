package com.irion.common.service;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntFunction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * 치지직 목록 캐시. 이 저장소에서 제일 손대기 어려운 코드다 —
 * TTL 폴백, 커서 페이징, 확장 중복 방지, 무한루프 차단이 한 클래스에 모여 있다.
 *
 * 여기서 지키려는 것은 두 가지다.
 * 하나는 <b>치지직이 죽어도 화면이 죽지 않는 것</b>(만료된 값으로 물러난다),
 * 다른 하나는 <b>한 요청이 외부 API 를 무제한으로 두드리지 못하는 것</b>이다.
 *
 * 외부 호출은 ChzzkClient 를 상속한 대역으로 갈음한다. 진짜 HTTP 는 ChzzkClientTest 가 본다.
 */
public class LiveFeedServiceTest {

    // ── 방송 상태 캐시 ────────────────────────────────────────

    @Test
    public void 캐시가_살아_있으면_외부를_다시_부르지_않는다() throws Exception {
        FakeChzzk chzzk = new FakeChzzk();
        chzzk.liveStatus = liveStatus(true);
        LiveFeedService service = serviceWith(chzzk);

        service.getLiveStatus();
        service.getLiveStatus();
        service.getLiveStatus();

        assertEquals("TTL 안에서는 한 번만 받아와야 한다", 1, chzzk.liveCalls.get());
    }

    @Test
    public void 캐시가_만료되면_다시_받아온다() throws Exception {
        FakeChzzk chzzk = new FakeChzzk();
        chzzk.liveStatus = liveStatus(true);
        LiveFeedService service = serviceWith(chzzk);

        service.getLiveStatus();
        expire(service, "liveStatusCache");
        service.getLiveStatus();

        assertEquals(2, chzzk.liveCalls.get());
    }

    /**
     * 이 클래스가 존재하는 이유. 치지직이 죽었을 때 화면에 "오류"가 아니라
     * 조금 낡은 값이 남아야 한다.
     */
    @Test
    public void 외부가_죽으면_만료된_값이라도_돌려준다() throws Exception {
        FakeChzzk chzzk = new FakeChzzk();
        chzzk.liveStatus = liveStatus(true);
        LiveFeedService service = serviceWith(chzzk);

        service.getLiveStatus();
        expire(service, "liveStatusCache");
        chzzk.liveStatus = null; // 치지직 장애

        Map<String, Object> fallback = service.getLiveStatus();

        assertNotNull("만료된 값으로 물러나야 한다 — null 이면 화면이 오류로 떨어진다", fallback);
        assertEquals(Boolean.TRUE, fallback.get("isLive"));
    }

    /** 로더가 터져도 마찬가지다 — 예외가 호출부까지 올라가면 안 된다 */
    @Test
    public void 로더가_예외를_던져도_만료된_값으로_물러난다() throws Exception {
        FakeChzzk chzzk = new FakeChzzk();
        chzzk.liveStatus = liveStatus(true);
        LiveFeedService service = serviceWith(chzzk);

        service.getLiveStatus();
        expire(service, "liveStatusCache");
        chzzk.liveFailure = new RuntimeException("치지직 타임아웃");

        Map<String, Object> fallback = service.getLiveStatus();

        assertNotNull("예외가 나도 만료된 값이 남아야 한다", fallback);
        assertEquals(Boolean.TRUE, fallback.get("isLive"));
    }

    /** 물러날 값조차 없는 첫 호출이면 null 이다. 호출부가 실패 응답을 만든다 */
    @Test
    public void 물러날_값이_없으면_null_이다() throws Exception {
        FakeChzzk chzzk = new FakeChzzk();
        chzzk.liveStatus = null;

        assertNull(serviceWith(chzzk).getLiveStatus());
    }

    /** 갱신은 한 스레드만 — 나머지는 그 결과를 나눠 쓴다 */
    @Test
    public void 동시에_몰려도_외부는_한_번만_부른다() throws Exception {
        FakeChzzk chzzk = new FakeChzzk();
        chzzk.liveStatus = liveStatus(true);
        chzzk.liveDelayMillis = 50; // 겹칠 틈을 만든다
        LiveFeedService service = serviceWith(chzzk);

        int threads = 12;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        List<Thread> pool = new ArrayList<Thread>();

        for (int i = 0; i < threads; i++) {
            Thread thread = new Thread(() -> {
                try {
                    start.await();
                    service.getLiveStatus();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
            pool.add(thread);
            thread.start();
        }

        start.countDown();
        assertTrue("스레드가 끝나지 않았다", done.await(10, TimeUnit.SECONDS));
        for (Thread thread : pool) {
            thread.join();
        }

        assertEquals(threads + " 개가 동시에 들어와도 외부 호출은 한 번이어야 한다",
                1, chzzk.liveCalls.get());
    }

    // ── 클립 ──────────────────────────────────────────────────

    /** 첫 요청에 미리 두 페이지를 담아둔다 (CLIP_INITIAL_PAGES) */
    @Test
    public void 클립_첫_로드는_두_페이지를_받는다() throws Exception {
        FakeChzzk chzzk = new FakeChzzk();
        chzzk.clipPage = endlessClipPages();
        LiveFeedService service = serviceWith(chzzk);

        LiveFeedService.ClipFeed feed = service.getClips(6);

        assertEquals(2, chzzk.clipCalls.get());
        assertEquals(2 * ChzzkClient.CLIP_PAGE_SIZE, feed.getClips().size());
        assertNull("첫 페이지는 커서 없이 부른다", chzzk.clipCursors.get(0));
        assertNotNull("두 번째부터는 커서를 실어야 한다", chzzk.clipCursors.get(1));
    }

    @Test
    public void 필요한_만큼만_이어_받는다() throws Exception {
        FakeChzzk chzzk = new FakeChzzk();
        chzzk.clipPage = endlessClipPages();
        LiveFeedService service = serviceWith(chzzk);

        int loaded = 2 * ChzzkClient.CLIP_PAGE_SIZE;
        LiveFeedService.ClipFeed feed = service.getClips(loaded + 1);

        assertEquals("한 페이지만 더 받으면 된다", 3, chzzk.clipCalls.get());
        assertEquals(loaded + ChzzkClient.CLIP_PAGE_SIZE, feed.getClips().size());
    }

    /**
     * 상한이 없으면 ?offset=2999 하나로 외부 API 를 수십 번 부르게 만들 수 있다.
     * 한 요청이 낼 수 있는 호출은 CLIP_PAGES_PER_REQUEST 까지다.
     */
    @Test
    public void 한_요청이_낼_수_있는_외부_호출에_상한이_있다() throws Exception {
        FakeChzzk chzzk = new FakeChzzk();
        chzzk.clipPage = endlessClipPages();
        LiveFeedService service = serviceWith(chzzk);

        LiveFeedService.ClipFeed feed = service.getClips(LiveFeedService.CLIP_MAX);

        assertEquals("첫 로드 2 + 확장 10 이 상한이다", 12, chzzk.clipCalls.get());
        assertEquals(12 * ChzzkClient.CLIP_PAGE_SIZE, feed.getClips().size());
        assertTrue("아직 더 남았다고 알려야 한다", feed.canGrow());
    }

    /** 같은 클립이 두 페이지에 걸쳐 와도 한 번만 담는다 */
    @Test
    public void 같은_클립은_한_번만_담는다() throws Exception {
        FakeChzzk chzzk = new FakeChzzk();
        chzzk.clipPage = call -> (call == 0)
                ? clipPage(0, 10, "cursor-1")
                : clipPage(5, 10, "cursor-2"); // 5..9 가 겹친다

        LiveFeedService service = serviceWith(chzzk);

        assertEquals(15, service.getClips(6).getClips().size());
    }

    /**
     * 커서가 돌지 않고 같은 페이지가 계속 오면 상한까지 헛돈다.
     * 진전이 없으면 그 자리에서 멈춰야 한다.
     */
    @Test
    public void 커서가_돌지_않으면_그_자리에서_멈춘다() throws Exception {
        FakeChzzk chzzk = new FakeChzzk();
        // 첫 두 페이지는 정상, 그 뒤로는 이미 본 것만 계속 돌려준다
        chzzk.clipPage = call -> (call < 2)
                ? clipPage(call * ChzzkClient.CLIP_PAGE_SIZE, ChzzkClient.CLIP_PAGE_SIZE, "cursor")
                : clipPage(0, ChzzkClient.CLIP_PAGE_SIZE, "cursor");

        LiveFeedService service = serviceWith(chzzk);
        service.getClips(LiveFeedService.CLIP_MAX);

        assertEquals("진전 없는 페이지를 받으면 즉시 멈춰야 한다 (상한 10 까지 헛돌면 12)",
                3, chzzk.clipCalls.get());
    }

    /** 빈 목록을 캐시하면 장애가 10분간 굳는다 — 다음 요청이 다시 시도해야 한다 */
    @Test
    public void 클립이_비면_캐시하지_않는다() throws Exception {
        FakeChzzk chzzk = new FakeChzzk();
        chzzk.clipPage = call -> null; // 치지직 장애
        LiveFeedService service = serviceWith(chzzk);

        assertNull(service.getClips(6));
        int afterFirst = chzzk.clipCalls.get();

        assertNull(service.getClips(6));
        assertTrue("빈 결과가 캐시되면 안 된다 — 다음 요청이 다시 시도해야 한다",
                chzzk.clipCalls.get() > afterFirst);
    }

    /** 확장이 실패해도 이미 받아둔 목록은 지키고 있어야 한다 */
    @Test
    public void 확장이_실패해도_받아둔_목록은_남는다() throws Exception {
        FakeChzzk chzzk = new FakeChzzk();
        chzzk.clipPage = call -> (call < 2)
                ? clipPage(call * ChzzkClient.CLIP_PAGE_SIZE, ChzzkClient.CLIP_PAGE_SIZE, "cursor")
                : null; // 확장 시점에 치지직이 죽는다

        LiveFeedService service = serviceWith(chzzk);
        int loaded = 2 * ChzzkClient.CLIP_PAGE_SIZE;

        assertEquals(loaded, service.getClips(loaded + 50).getClips().size());
    }

    // ── 다시보기 ──────────────────────────────────────────────

    /** 한 페이지에 맞추면 쌓였을 때 잘린다 — 마지막 페이지까지 이어 받는다 */
    @Test
    public void 다시보기는_마지막_페이지까지_이어_받는다() throws Exception {
        FakeChzzk chzzk = new FakeChzzk();
        chzzk.videoPage = page -> (page == 0)
                ? videos(0, ChzzkClient.VIDEO_PAGE_SIZE)  // 꽉 찬 페이지 — 더 있다
                : videos(100, 20);                        // 덜 찬 페이지 — 여기가 끝

        LiveFeedService service = serviceWith(chzzk);

        assertEquals(ChzzkClient.VIDEO_PAGE_SIZE + 20, service.getVideos().size());
        assertEquals("덜 찬 페이지를 만나면 멈춘다", 2, chzzk.videoCalls.get());
    }

    /** 페이지가 끝없이 꽉 차 와도 VIDEO_MAX_PAGES 에서 끊는다 */
    @Test
    public void 다시보기_페이지_수에_상한이_있다() throws Exception {
        FakeChzzk chzzk = new FakeChzzk();
        chzzk.videoPage = page -> videos(page * ChzzkClient.VIDEO_PAGE_SIZE,
                ChzzkClient.VIDEO_PAGE_SIZE);

        LiveFeedService service = serviceWith(chzzk);
        service.getVideos();

        assertEquals(20, chzzk.videoCalls.get());
    }

    /** HIDDEN_VIDEO_NOS — 지우는 것이 아니라 이 사이트에서만 가린다 */
    @Test
    public void 감춘_다시보기는_목록에서_빠진다() throws Exception {
        FakeChzzk chzzk = new FakeChzzk();
        chzzk.videoPage = page -> (page == 0)
                ? list(video("319019"), video("100"), video("101"))
                : null;

        List<Map<String, Object>> videos = serviceWith(chzzk).getVideos();

        assertEquals(2, videos.size());
        assertFalse("감춘 videoNo 가 목록에 남아 있다", videoNos(videos).contains("319019"));
        assertTrue(videoNos(videos).contains("100"));
    }

    @Test
    public void 같은_다시보기는_한_번만_담는다() throws Exception {
        FakeChzzk chzzk = new FakeChzzk();
        chzzk.videoPage = page -> (page == 0)
                ? videos(0, ChzzkClient.VIDEO_PAGE_SIZE)
                : videos(ChzzkClient.VIDEO_PAGE_SIZE - 10, 20); // 10개가 겹친다

        assertEquals(ChzzkClient.VIDEO_PAGE_SIZE + 10, serviceWith(chzzk).getVideos().size());
    }

    /** 첫 페이지부터 실패했으면 만료된 캐시로 폴백시켜야 한다 */
    @Test
    public void 다시보기가_비면_캐시하지_않는다() throws Exception {
        FakeChzzk chzzk = new FakeChzzk();
        chzzk.videoPage = page -> null;
        LiveFeedService service = serviceWith(chzzk);

        assertNull(service.getVideos());
        int afterFirst = chzzk.videoCalls.get();

        assertNull(service.getVideos());
        assertTrue("빈 결과가 캐시되면 안 된다", chzzk.videoCalls.get() > afterFirst);
    }

    @Test
    public void 다시보기도_외부가_죽으면_만료된_값을_돌려준다() throws Exception {
        FakeChzzk chzzk = new FakeChzzk();
        chzzk.videoPage = page -> (page == 0) ? videos(0, 3) : null;
        LiveFeedService service = serviceWith(chzzk);

        assertEquals(3, service.getVideos().size());

        expire(service, "videosCache");
        chzzk.videoPage = page -> null; // 치지직 장애

        List<Map<String, Object>> fallback = service.getVideos();
        assertNotNull("만료된 값으로 물러나야 한다", fallback);
        assertEquals(3, fallback.size());
    }


    // ── 대역 ──────────────────────────────────────────────────

    /** ChzzkClient 를 상속해 호출만 가로챈다. 어떤 커서로 불렸는지도 남긴다 */
    private static final class FakeChzzk extends ChzzkClient {

        final AtomicInteger liveCalls = new AtomicInteger();
        final AtomicInteger clipCalls = new AtomicInteger();
        final AtomicInteger videoCalls = new AtomicInteger();

        /** 클립 페이지를 부를 때 실린 clipUID (첫 페이지는 null) */
        final List<String> clipCursors = new ArrayList<String>();

        Map<String, Object> liveStatus;
        RuntimeException liveFailure;
        long liveDelayMillis;

        /** 호출 순번 → 페이지. null 이면 실패 */
        IntFunction<ChzzkClient.ClipPage> clipPage = call -> null;

        /** 페이지 번호 → 목록. null 이면 실패 */
        IntFunction<List<Map<String, Object>>> videoPage = page -> null;

        @Override
        public Map<String, Object> fetchLiveStatus() {
            liveCalls.incrementAndGet();
            if (liveDelayMillis > 0) {
                try {
                    Thread.sleep(liveDelayMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            if (liveFailure != null) {
                throw liveFailure;
            }
            return liveStatus;
        }

        @Override
        public ChzzkClient.ClipPage fetchClipPage(String clipUID, String readCount) {
            synchronized (clipCursors) {
                clipCursors.add(clipUID);
            }
            return clipPage.apply(clipCalls.getAndIncrement());
        }

        @Override
        public List<Map<String, Object>> fetchVideoPage(int page) {
            videoCalls.incrementAndGet();
            return videoPage.apply(page);
        }
    }


    // ── 거들 ──────────────────────────────────────────────────

    private static LiveFeedService serviceWith(FakeChzzk chzzk) throws Exception {
        LiveFeedService service = new LiveFeedService();
        Field field = LiveFeedService.class.getDeclaredField("chzzk");
        field.setAccessible(true);
        field.set(service, chzzk);
        return service;
    }

    /**
     * 캐시를 만료시킨다. TTL 이 1~10분이라 기다릴 수 없어 적재 시각만 과거로 돌린다.
     * Snapshot 은 값과 시각을 함께 들고 있으므로 값은 그대로 두고 새로 만든다.
     */
    @SuppressWarnings("unchecked")
    private static void expire(LiveFeedService service, String cacheField) throws Exception {
        Field field = LiveFeedService.class.getDeclaredField(cacheField);
        field.setAccessible(true);
        AtomicReference<Object> ref = (AtomicReference<Object>) field.get(service);

        Object snapshot = ref.get();
        assertNotNull(cacheField + " 이 아직 비어 있다 — 만료시킬 값이 없다", snapshot);

        Class<?> snapshotType = snapshot.getClass();
        Field valueField = snapshotType.getDeclaredField("value");
        valueField.setAccessible(true);

        Constructor<?> constructor = snapshotType.getDeclaredConstructor(Object.class, long.class);
        constructor.setAccessible(true);

        // 가장 긴 TTL(10분)보다 확실히 오래 전으로
        long longAgo = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1);
        ref.set(constructor.newInstance(valueField.get(snapshot), longAgo));
    }

    /** 매 호출마다 새 클립 50개와 다음 커서를 주는, 끝나지 않는 목록 */
    private static IntFunction<ChzzkClient.ClipPage> endlessClipPages() {
        return call -> clipPage(call * ChzzkClient.CLIP_PAGE_SIZE,
                ChzzkClient.CLIP_PAGE_SIZE, "cursor-" + call);
    }

    private static ChzzkClient.ClipPage clipPage(int from, int count, String nextClipUID) {
        List<Map<String, Object>> clips = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < count; i++) {
            Map<String, Object> clip = new HashMap<String, Object>();
            clip.put("clipId", "clip-" + (from + i));
            clip.put("clipTitle", "클립 " + (from + i));
            clips.add(clip);
        }
        return new ChzzkClient.ClipPage(clips, nextClipUID, "100");
    }

    private static List<Map<String, Object>> videos(int from, int count) {
        List<Map<String, Object>> videos = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < count; i++) {
            videos.add(video(String.valueOf(from + i)));
        }
        return videos;
    }

    private static Map<String, Object> video(String videoNo) {
        Map<String, Object> video = new HashMap<String, Object>();
        video.put("videoNo", videoNo);
        video.put("videoTitle", "다시보기 " + videoNo);
        return video;
    }

    private static List<String> videoNos(List<Map<String, Object>> videos) {
        List<String> nos = new ArrayList<String>();
        for (Map<String, Object> video : videos) {
            nos.add((String) video.get("videoNo"));
        }
        return nos;
    }

    private static Map<String, Object> liveStatus(boolean isLive) {
        Map<String, Object> status = new HashMap<String, Object>();
        status.put("isLive", isLive);
        status.put("channelId", ChzzkClient.CHANNEL_ID);
        return status;
    }

    @SafeVarargs
    private static <T> List<T> list(T... items) {
        List<T> result = new ArrayList<T>();
        for (T item : items) {
            result.add(item);
        }
        return result;
    }
}

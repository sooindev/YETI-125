package com.irion.common.controller;

import com.irion.common.util.JsonResult;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@Controller
@RequestMapping("/live")
public class LiveController {

    private static final String CHANNEL_ID = "63368ec9081dc85e61d0e4310b7e1602";
    private static final String CHZZK_API = "https://api.chzzk.naver.com/service/v3/channels/" + CHANNEL_ID + "/live-detail";
    private static final long CACHE_DURATION = 10 * 60 * 1000; // 10분 (클립/비디오)
    private static final long LIVE_CACHE_DURATION = 1 * 60 * 1000; // 1분 (방송 상태)

    // 캐시
    // 컨트롤러는 싱글턴이라 여러 요청 스레드가 동시에 접근한다.
    // 값과 적재 시각을 스냅샷 하나로 묶어 참조만 교체하고, 갱신은
    // 락으로 한 스레드에만 맡긴다. (아래 cached() 참고)
    private final AtomicReference<Snapshot<Map<String, Object>>> liveStatusCache = new AtomicReference<>();
    private final AtomicReference<Snapshot<List<Map<String, Object>>>> clipsCache = new AtomicReference<>();
    private final AtomicReference<Snapshot<List<Map<String, Object>>>> videosCache = new AtomicReference<>();

    private final Object liveStatusLock = new Object();
    private final Object clipsLock = new Object();
    private final Object videosLock = new Object();

    // ========================================
    // API 엔드포인트
    // ========================================

    /** 방송 상태 조회 */
    @GetMapping("/status")
    @ResponseBody
    public JsonResult getLiveStatus() {
        Map<String, Object> data = cached(liveStatusCache, liveStatusLock,
                LIVE_CACHE_DURATION, this::loadLiveStatus);

        if (data == null) {
            return JsonResult.fail("방송 상태 확인 중 오류 발생");
        }
        return JsonResult.success("조회 성공", data);
    }

    /** 클립 목록 조회 (인기순) */
    @GetMapping("/clips")
    @ResponseBody
    public JsonResult getClips(@RequestParam(defaultValue = "6") int limit, @RequestParam(defaultValue = "0") int offset) {
        List<Map<String, Object>> all = cached(clipsCache, clipsLock,
                CACHE_DURATION, this::loadClips);

        if (all == null) {
            return JsonResult.fail("클립 조회 중 오류 발생");
        }
        return JsonResult.success("조회 성공", paginate(all, "clips", offset, limit));
    }

    /** 다시보기 목록 조회 */
    @GetMapping("/videos")
    @ResponseBody
    public JsonResult getVideos(@RequestParam(defaultValue = "6") int limit, @RequestParam(defaultValue = "0") int offset) {
        List<Map<String, Object>> all = cached(videosCache, videosLock,
                CACHE_DURATION, this::loadVideos);

        if (all == null) {
            return JsonResult.fail("다시보기 조회 중 오류 발생");
        }
        return JsonResult.success("조회 성공", paginate(all, "videos", offset, limit));
    }

    // ========================================
    // 데이터 로드
    // ========================================

    /** 방송 상태 로드. 실패하면 null 을 돌려 만료된 캐시로 폴백시킨다. */
    private Map<String, Object> loadLiveStatus() {
        String json = fetchApi(CHZZK_API);
        if (json == null)
            return null;

        boolean isLive = json.contains("\"status\":\"OPEN\"");

        Map<String, Object> data = new HashMap<>();
        data.put("isLive", isLive);
        data.put("channelId", CHANNEL_ID);
        data.put("channelUrl", "https://chzzk.naver.com/live/" + CHANNEL_ID);

        if (isLive) {
            data.put("liveTitle", extractString(json, "liveTitle"));
            data.put("thumbnail", extractString(json, "liveImageUrl").replace("{type}", "480"));
            data.put("viewerCount", extractNumber(json, "concurrentUserCount"));
        }

        return data;
    }

    /** 인기 클립 로드 (최대 100개) */
    private List<Map<String, Object>> loadClips() {
        List<Map<String, Object>> clips = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        String next = null;

        for (int page = 0; page < 10 && clips.size() < 100; page++) {
            String apiUrl = "https://api.chzzk.naver.com/service/v1/channels/" + CHANNEL_ID
                    + "/clips?filterType=ALL&orderType=POPULAR&size=20"
                    + (next != null ? "&next=" + next : "");

            String json = fetchApi(apiUrl);
            if (json == null)
                break;

            // 클립 파싱
            for (Map<String, Object> clip : parseArray(json, "clipUID", this::parseClip)) {
                String id = (String) clip.get("clipId");
                if (id != null && !ids.contains(id)) {
                    ids.add(id);
                    clips.add(clip);
                }
            }

            // 다음 페이지
            next = extractNextPage(json);
            if (next == null)
                break;
        }

        // 한 건도 못 모았으면 API 장애로 보고 실패를 알린다.
        // (빈 목록을 캐시해 두면 장애가 10분간 굳어버린다)
        if (clips.isEmpty())
            return null;

        return clips.size() > 100 ? clips.subList(0, 100) : clips;
    }

    /** 다시보기 로드 */
    private List<Map<String, Object>> loadVideos() {
        String apiUrl = "https://api.chzzk.naver.com/service/v1/channels/" + CHANNEL_ID
                + "/videos?sortType=LATEST&pagingType=PAGE&page=0&size=50";

        String json = fetchApi(apiUrl);
        if (json == null)
            return null;

        return parseArray(json, "videoNo", this::parseVideo);
    }

    // ========================================
    // 파싱 메서드
    // ========================================

    /** 클립 파싱 */
    private Map<String, Object> parseClip(String json) {
        String clipUID = extractString(json, "clipUID");
        if (clipUID == null || clipUID.isEmpty())
            return null;

        Map<String, Object> clip = new HashMap<>();
        clip.put("clipId", clipUID);
        clip.put("clipTitle", extractString(json, "clipTitle"));
        clip.put("thumbnailUrl", extractString(json, "thumbnailImageUrl"));
        clip.put("viewCount", extractNumber(json, "readCount"));
        clip.put("duration", extractNumber(json, "duration"));
        clip.put("createdAt", extractString(json, "createdDate"));
        clip.put("clipUrl", "https://chzzk.naver.com/clips/" + clipUID);
        return clip;
    }

    /** 비디오 파싱 */
    private Map<String, Object> parseVideo(String json) {
        String videoNo = extractNumber(json, "videoNo");
        if (videoNo == null || videoNo.isEmpty())
            return null;

        Map<String, Object> video = new HashMap<>();
        video.put("videoNo", videoNo);
        video.put("videoTitle", extractString(json, "videoTitle"));
        video.put("thumbnailUrl", extractString(json, "thumbnailImageUrl"));
        video.put("duration", extractNumber(json, "duration"));
        video.put("readCount", extractNumber(json, "readCount"));
        video.put("publishDate", extractString(json, "publishDate"));
        video.put("videoUrl", "https://chzzk.naver.com/video/" + videoNo);
        return video;
    }

    // ========================================
    // 유틸리티 메서드
    // ========================================

    /** API 호출 */
    private String fetchApi(String apiUrl) {
        try {
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() != 200)
                return null;

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();

            return sb.toString();

        } catch (Exception e) {
            return null;
        }
    }

    /** 페이지네이션 결과 생성 */
    private Map<String, Object> paginate(List<Map<String, Object>> list, String key, int offset, int limit) {
        Map<String, Object> result = new HashMap<>();

        if (list == null || list.isEmpty()) {
            result.put(key, new ArrayList<>());
            result.put("hasMore", false);
            return result;
        }

        int endIndex = Math.min(offset + limit, list.size());
        result.put(key, list.subList(offset, endIndex));
        result.put("hasMore", endIndex < list.size());
        result.put("nextOffset", endIndex);

        return result;
    }

    // ========================================
    // 캐시
    // ========================================

    /**
     * 값과 적재 시각을 함께 담는 불변 스냅샷.
     * 둘을 각각 필드로 두면 "새 값 + 옛 시각" 같은 어긋난 조합이 잠깐
     * 보일 수 있다. 참조 하나만 통째로 바꾸면 그런 틈이 생기지 않는다.
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
     * 캐시에서 읽되, 만료됐으면 갱신한다.
     *
     * 갱신은 락으로 묶어 한 스레드만 수행한다. 캐시가 만료되는 순간
     * 요청이 몰려도 외부 API 는 한 번만 호출된다. 나머지 스레드는
     * 잠깐 기다렸다가 갱신된 값을 그대로 받는다.
     *
     * 갱신에 실패하면(로더가 null 을 주거나 예외를 던지면) 만료된 값이라도
     * 돌려준다. 외부 API 가 흔들려도 화면이 비지 않게 하기 위해서다.
     * 값이 아예 없고 적재도 실패한 경우에만 null 이다.
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

    /** JSON 배열 파싱 */
    private List<Map<String, Object>> parseArray(String json, String idKey, java.util.function.Function<String, Map<String, Object>> parser) {
        List<Map<String, Object>> list = new ArrayList<>();

        int dataStart = json.indexOf("\"data\":[");
        if (dataStart == -1)
            return list;

        int arrayStart = json.indexOf("[", dataStart);
        int arrayEnd = findBracket(json, arrayStart, '[', ']');
        if (arrayStart == -1 || arrayEnd == -1)
            return list;

        String dataArray = json.substring(arrayStart + 1, arrayEnd);
        int pos = 0;

        while (true) {
            int objStart = dataArray.indexOf("{", pos);
            if (objStart == -1) break;

            int objEnd = findBracket(dataArray, objStart, '{', '}');
            if (objEnd == -1) break;

            String objJson = dataArray.substring(objStart, objEnd + 1);
            Map<String, Object> item = parser.apply(objJson);
            if (item != null) {
                list.add(item);
            }

            pos = objEnd + 1;
        }

        return list;
    }

    /** 다음 페이지 토큰 추출 */
    private String extractNextPage(String json) {
        int nextStart = json.indexOf("\"next\":{");
        if (nextStart == -1)
            return null;

        int clipStart = json.indexOf("\"clipUID\":\"", nextStart);
        if (clipStart == -1)
            return null;

        int valueStart = clipStart + 11;
        int valueEnd = json.indexOf("\"", valueStart);

        return (valueEnd > valueStart) ? json.substring(valueStart, valueEnd) : null;
    }

    /** 문자열 값 추출 */
    private String extractString(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1)
            return "";

        start += search.length();
        int end = json.indexOf("\"", start);
        String value = (end > start) ? json.substring(start, end) : "";

        // 유니코드 이스케이프 시퀀스 디코딩 (XXXX -> 실제 문자)
        return decodeUnicode(value);
    }

    /** 유니코드 이스케이프 시퀀스 디코딩 */
    private String decodeUnicode(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        StringBuilder sb = new StringBuilder();
        int length = str.length();

        for (int i = 0; i < length; i++) {
            char ch = str.charAt(i);

            // XXXX 형태의 유니코드 이스케이프 시퀀스 확인
            if (ch == '\\' && i + 1 < length && str.charAt(i + 1) == 'u') {
                // 다음 4자리가 16진수인지 확인
                if (i + 5 < length) {
                    try {
                        String hex = str.substring(i + 2, i + 6);
                        int code = Integer.parseInt(hex, 16);
                        sb.append((char) code);
                        i += 5; // XXXX 전체를 건너뜀
                        continue;
                    } catch (NumberFormatException e) {
                        // 16진수가 아니면 그대로 추가
                    }
                }
            }

            sb.append(ch);
        }

        return sb.toString();
    }

    /** 숫자 값 추출 */
    private String extractNumber(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1)
            return "";

        start += search.length();
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) {
            end++;
        }

        return (end > start) ? json.substring(start, end) : "";
    }

    /** 괄호 매칭 찾기 */
    private int findBracket(String json, int start, char open, char close) {
        int count = 0;
        boolean inString = false;

        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);

            // 따옴표 escape 판정: 앞쪽 연속 백슬래시 개수가 홀수면 escape된 따옴표
            if (c == '"' && !isEscaped(json, i)) {
                inString = !inString;
            }

            if (!inString) {
                if (c == open) count++;
                else if (c == close) count--;
                if (count == 0) return i;
            }
        }

        return -1;
    }

    /** 해당 위치의 문자가 escape 되었는지 (앞쪽 연속 백슬래시 홀수 개) */
    private boolean isEscaped(String str, int index) {
        int backslashes = 0;
        for (int i = index - 1; i >= 0 && str.charAt(i) == '\\'; i--) {
            backslashes++;
        }
        return (backslashes % 2) == 1;
    }
}
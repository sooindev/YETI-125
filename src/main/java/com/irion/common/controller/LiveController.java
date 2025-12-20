package com.irion.common.controller;

import com.irion.common.util.JsonResult;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

@Controller
@RequestMapping("/live")
public class LiveController {

    private static final String CHANNEL_ID = "63368ec9081dc85e61d0e4310b7e1602";
    private static final String CHZZK_API = "https://api.chzzk.naver.com/service/v3/channels/" + CHANNEL_ID + "/live-detail";
    private static final long CACHE_DURATION = 10 * 60 * 1000; // 10분

    // 캐시
    private List<Map<String, Object>> cachedClips = null;
    private List<Map<String, Object>> cachedVideos = null;
    private long clipsCacheTime = 0;
    private long videosCacheTime = 0;

    // ========================================
    // API 엔드포인트
    // ========================================

    /** 방송 상태 조회 */
    @GetMapping("/status")
    @ResponseBody
    public JsonResult getLiveStatus() {
        try {
            String json = fetchApi(CHZZK_API);
            if (json == null) {
                return JsonResult.fail("API 호출 실패");
            }

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

            return JsonResult.success("조회 성공", data);

        } catch (Exception e) {
            return JsonResult.fail("방송 상태 확인 중 오류 발생");
        }
    }

    /** 클립 목록 조회 (인기순) */
    @GetMapping("/clips")
    @ResponseBody
    public JsonResult getClips(@RequestParam(defaultValue = "6") int limit, @RequestParam(defaultValue = "0") int offset) {
        try {
            // 캐시 갱신
            if (cachedClips == null || isExpired(clipsCacheTime)) {
                cachedClips = loadClips();
                clipsCacheTime = System.currentTimeMillis();
            }

            return JsonResult.success("조회 성공", paginate(cachedClips, "clips", offset, limit));

        } catch (Exception e) {
            return JsonResult.fail("클립 조회 중 오류 발생");
        }
    }

    /** 다시보기 목록 조회 */
    @GetMapping("/videos")
    @ResponseBody
    public JsonResult getVideos(@RequestParam(defaultValue = "6") int limit, @RequestParam(defaultValue = "0") int offset) {
        try {
            // 캐시 갱신
            if (cachedVideos == null || isExpired(videosCacheTime)) {
                cachedVideos = loadVideos();
                videosCacheTime = System.currentTimeMillis();
            }

            return JsonResult.success("조회 성공", paginate(cachedVideos, "videos", offset, limit));

        } catch (Exception e) {
            return JsonResult.fail("다시보기 조회 중 오류 발생");
        }
    }

    // ========================================
    // 데이터 로드
    // ========================================

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

        return clips.size() > 100 ? clips.subList(0, 100) : clips;
    }

    /** 다시보기 로드 */
    private List<Map<String, Object>> loadVideos() {
        String apiUrl = "https://api.chzzk.naver.com/service/v1/channels/" + CHANNEL_ID
                + "/videos?sortType=LATEST&pagingType=PAGE&page=0&size=50";

        String json = fetchApi(apiUrl);
        if (json == null)
            return new ArrayList<>();

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

    /** 캐시 만료 확인 */
    private boolean isExpired(long cacheTime) {
        return System.currentTimeMillis() - cacheTime > CACHE_DURATION;
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

            if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
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
}
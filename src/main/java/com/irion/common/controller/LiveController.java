package com.irion.common.controller;

import com.irion.common.util.JsonResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

@Controller
@RequestMapping("/live")
public class LiveController {

    private static final Logger logger = LoggerFactory.getLogger(LiveController.class);

    private static final String CHANNEL_ID = "63368ec9081dc85e61d0e4310b7e1602";
    private static final String CHZZK_API_URL = "https://api.chzzk.naver.com/service/v3/channels/" + CHANNEL_ID + "/live-detail";

    // 클립 캐시
    private List<Map<String, Object>> popularClips = null;
    private long clipCacheTime = 0;
    private static final long CLIP_CACHE_DURATION = 10 * 60 * 1000; // 10분

    // 다시보기 캐시
    private List<Map<String, Object>> recentVideos = null;
    private long videoCacheTime = 0;
    private static final long VIDEO_CACHE_DURATION = 10 * 60 * 1000; // 10분

    // 방송 상태 조회
    @GetMapping("/status")
    @ResponseBody
    public JsonResult getLiveStatus() {
        try {
            URL url = new URL(CHZZK_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();

            if (responseCode == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                br.close();

                String jsonResponse = response.toString();

                Map<String, Object> data = new HashMap<>();

                boolean isLive = jsonResponse.contains("\"status\":\"OPEN\"");
                data.put("isLive", isLive);
                data.put("channelId", CHANNEL_ID);
                data.put("channelUrl", "https://chzzk.naver.com/live/" + CHANNEL_ID);

                if (isLive) {
                    String liveTitle = extractJsonValue(jsonResponse, "liveTitle");
                    if (liveTitle != null) {
                        data.put("liveTitle", liveTitle);
                    }

                    String thumbnail = extractJsonValue(jsonResponse, "liveImageUrl");
                    if (thumbnail != null) {
                        thumbnail = thumbnail.replace("{type}", "480");
                        data.put("thumbnail", thumbnail);
                    }

                    String viewerCount = extractJsonNumber(jsonResponse, "concurrentUserCount");
                    if (viewerCount != null) {
                        data.put("viewerCount", viewerCount);
                    }
                }

                return JsonResult.success("조회 성공", data);
            } else {
                return JsonResult.fail("API 호출 실패");
            }

        } catch (Exception e) {
            logger.error("Error checking live status", e);
            return JsonResult.fail("방송 상태 확인 중 오류 발생");
        }
    }

    // 클립 목록 조회 (인기순)
    @GetMapping("/clips")
    @ResponseBody
    public JsonResult getClips(@RequestParam(defaultValue = "6") int limit,
                               @RequestParam(defaultValue = "0") int offset) {
        try {
            // 캐시 확인
            long now = System.currentTimeMillis();
            if (popularClips == null || (now - clipCacheTime) > CLIP_CACHE_DURATION) {
                loadPopularClips();
                clipCacheTime = now;
            }

            Map<String, Object> result = new HashMap<>();

            if (popularClips == null || popularClips.isEmpty()) {
                result.put("clips", new ArrayList<>());
                result.put("hasMore", false);
                return JsonResult.success("조회 성공", result);
            }

            // offset부터 limit개 반환
            int endIndex = Math.min(offset + limit, popularClips.size());
            List<Map<String, Object>> clips = new ArrayList<>();

            for (int i = offset; i < endIndex; i++) {
                clips.add(popularClips.get(i));
            }

            result.put("clips", clips);
            result.put("hasMore", endIndex < popularClips.size());
            result.put("nextOffset", endIndex);

            return JsonResult.success("조회 성공", result);

        } catch (Exception e) {
            logger.error("Error fetching clips", e);
            return JsonResult.fail("클립 조회 중 오류 발생");
        }
    }

    // 인기순 클립 100개 로드
    private void loadPopularClips() {
        List<Map<String, Object>> allClips = new ArrayList<>();
        Set<String> clipIds = new HashSet<>();
        String nextPage = null;

        try {
            // 최대 100개까지 로드
            for (int page = 0; page < 10; page++) {
                String clipApiUrl = "https://api.chzzk.naver.com/service/v1/channels/" + CHANNEL_ID
                        + "/clips?filterType=ALL&orderType=POPULAR&size=20";

                if (nextPage != null) {
                    clipApiUrl += "&next=" + nextPage;
                }

                URL url = new URL(clipApiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                if (conn.getResponseCode() != 200) break;

                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                br.close();

                String jsonResponse = response.toString();

                // 클립 파싱
                List<Map<String, Object>> clips = parseClips(jsonResponse);
                if (clips.isEmpty()) break;

                // 중복 제거
                for (Map<String, Object> clip : clips) {
                    String clipId = (String) clip.get("clipId");
                    if (clipId != null && !clipIds.contains(clipId)) {
                        clipIds.add(clipId);
                        allClips.add(clip);
                    }
                }

                // 100개 넘으면 중단
                if (allClips.size() >= 100) break;

                // 다음 페이지
                nextPage = extractNextPage(jsonResponse);
                if (nextPage == null) break;
            }

            // 최대 100개만 저장
            int maxSize = Math.min(100, allClips.size());
            popularClips = new ArrayList<>(allClips.subList(0, maxSize));

            logger.info("Loaded {} popular clips", popularClips.size());

        } catch (Exception e) {
            logger.error("Error loading popular clips", e);
            popularClips = new ArrayList<>();
        }
    }

    // 다음 페이지 추출
    private String extractNextPage(String json) {
        try {
            int nextStart = json.indexOf("\"next\":{");
            if (nextStart == -1) return null;

            int clipUIDStart = json.indexOf("\"clipUID\":\"", nextStart);
            if (clipUIDStart == -1) return null;

            int valueStart = clipUIDStart + 11;
            int valueEnd = json.indexOf("\"", valueStart);

            if (valueEnd > valueStart) {
                return json.substring(valueStart, valueEnd);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    // 클립 JSON 파싱
    private List<Map<String, Object>> parseClips(String json) {
        List<Map<String, Object>> clips = new ArrayList<>();

        try {
            int dataStart = json.indexOf("\"data\":[");
            if (dataStart == -1) return clips;

            int arrayStart = json.indexOf("[", dataStart);
            int arrayEnd = findMatchingBracket(json, arrayStart);
            if (arrayStart == -1 || arrayEnd == -1) return clips;

            String dataArray = json.substring(arrayStart + 1, arrayEnd);

            int clipStart = 0;
            while (true) {
                int objStart = dataArray.indexOf("{", clipStart);
                if (objStart == -1) break;

                int objEnd = findMatchingBrace(dataArray, objStart);
                if (objEnd == -1) break;

                String clipJson = dataArray.substring(objStart, objEnd + 1);

                String clipUID = extractJsonValue(clipJson, "clipUID");
                if (clipUID != null && !clipUID.isEmpty()) {
                    Map<String, Object> clip = new HashMap<>();
                    clip.put("clipId", clipUID);
                    clip.put("clipTitle", extractJsonValue(clipJson, "clipTitle"));
                    clip.put("thumbnailUrl", extractJsonValue(clipJson, "thumbnailImageUrl"));
                    clip.put("viewCount", extractJsonNumber(clipJson, "readCount"));
                    clip.put("duration", extractJsonNumber(clipJson, "duration"));
                    clip.put("createdAt", extractJsonValue(clipJson, "createdDate"));
                    clip.put("clipUrl", "https://chzzk.naver.com/clips/" + clipUID);
                    clips.add(clip);
                }

                clipStart = objEnd + 1;
            }

        } catch (Exception e) {
            logger.error("Error parsing clips", e);
        }

        return clips;
    }

    // JSON 문자열 값 추출
    private String extractJsonValue(String json, String key) {
        try {
            String searchKey = "\"" + key + "\":\"";
            int startIndex = json.indexOf(searchKey);
            if (startIndex == -1) return null;

            startIndex += searchKey.length();
            int endIndex = json.indexOf("\"", startIndex);
            if (endIndex == -1) return null;

            return json.substring(startIndex, endIndex);
        } catch (Exception e) {
            return null;
        }
    }

    // JSON 숫자 값 추출
    private String extractJsonNumber(String json, String key) {
        try {
            String searchKey = "\"" + key + "\":";
            int startIndex = json.indexOf(searchKey);
            if (startIndex == -1) return null;

            startIndex += searchKey.length();
            int endIndex = startIndex;
            while (endIndex < json.length() && Character.isDigit(json.charAt(endIndex))) {
                endIndex++;
            }

            if (endIndex == startIndex) return null;
            return json.substring(startIndex, endIndex);
        } catch (Exception e) {
            return null;
        }
    }

    // 매칭되는 대괄호 찾기
    private int findMatchingBracket(String json, int start) {
        int count = 0;
        for (int i = start; i < json.length(); i++) {
            if (json.charAt(i) == '[') count++;
            else if (json.charAt(i) == ']') count--;
            if (count == 0) return i;
        }
        return -1;
    }

    // 매칭되는 중괄호 찾기
    private int findMatchingBrace(String json, int start) {
        int count = 0;
        boolean inString = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                inString = !inString;
            }
            if (!inString) {
                if (c == '{') count++;
                else if (c == '}') count--;
                if (count == 0) return i;
            }
        }
        return -1;
    }

    // 다시보기 영상 조회
    @GetMapping("/videos")
    @ResponseBody
    public JsonResult getVideos(@RequestParam(defaultValue = "6") int limit,
                                @RequestParam(defaultValue = "0") int offset) {
        try {
            // 캐시 확인
            long now = System.currentTimeMillis();
            if (recentVideos == null || (now - videoCacheTime) > VIDEO_CACHE_DURATION) {
                loadRecentVideos();
                videoCacheTime = now;
            }

            Map<String, Object> result = new HashMap<>();

            if (recentVideos == null || recentVideos.isEmpty()) {
                result.put("videos", new ArrayList<>());
                result.put("hasMore", false);
                return JsonResult.success("조회 성공", result);
            }

            // offset부터 limit개 반환
            int endIndex = Math.min(offset + limit, recentVideos.size());
            List<Map<String, Object>> videos = new ArrayList<>();

            for (int i = offset; i < endIndex; i++) {
                videos.add(recentVideos.get(i));
            }

            result.put("videos", videos);
            result.put("hasMore", endIndex < recentVideos.size());
            result.put("nextOffset", endIndex);

            return JsonResult.success("조회 성공", result);

        } catch (Exception e) {
            logger.error("Error fetching videos", e);
            return JsonResult.fail("다시보기 조회 중 오류 발생");
        }
    }

    // 다시보기 영상 로드
    private void loadRecentVideos() {
        List<Map<String, Object>> allVideos = new ArrayList<>();

        try {
            String videoApiUrl = "https://api.chzzk.naver.com/service/v1/channels/" + CHANNEL_ID
                    + "/videos?sortType=LATEST&pagingType=PAGE&page=0&size=50";

            URL url = new URL(videoApiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                br.close();

                String jsonResponse = response.toString();
                allVideos = parseVideos(jsonResponse);
            }

            recentVideos = allVideos;
            logger.info("Loaded {} recent videos", recentVideos.size());

        } catch (Exception e) {
            logger.error("Error loading recent videos", e);
            recentVideos = new ArrayList<>();
        }
    }

    // 다시보기 JSON 파싱
    private List<Map<String, Object>> parseVideos(String json) {
        List<Map<String, Object>> videos = new ArrayList<>();

        try {
            int dataStart = json.indexOf("\"data\":[");
            if (dataStart == -1) return videos;

            int arrayStart = json.indexOf("[", dataStart);
            int arrayEnd = findMatchingBracket(json, arrayStart);
            if (arrayStart == -1 || arrayEnd == -1) return videos;

            String dataArray = json.substring(arrayStart + 1, arrayEnd);

            int videoStart = 0;
            while (true) {
                int objStart = dataArray.indexOf("{", videoStart);
                if (objStart == -1) break;

                int objEnd = findMatchingBrace(dataArray, objStart);
                if (objEnd == -1) break;

                String videoJson = dataArray.substring(objStart, objEnd + 1);

                String videoNo = extractJsonNumber(videoJson, "videoNo");
                if (videoNo != null && !videoNo.isEmpty()) {
                    Map<String, Object> video = new HashMap<>();
                    video.put("videoNo", videoNo);
                    video.put("videoTitle", extractJsonValue(videoJson, "videoTitle"));
                    video.put("thumbnailUrl", extractJsonValue(videoJson, "thumbnailImageUrl"));
                    video.put("duration", extractJsonNumber(videoJson, "duration"));
                    video.put("readCount", extractJsonNumber(videoJson, "readCount"));
                    video.put("publishDate", extractJsonValue(videoJson, "publishDate"));
                    video.put("videoUrl", "https://chzzk.naver.com/" + CHANNEL_ID + "/video/" + videoNo);
                    videos.add(video);
                }

                videoStart = objEnd + 1;
            }

        } catch (Exception e) {
            logger.error("Error parsing videos", e);
        }

        return videos;
    }

}
package com.irion.common.controller;

import com.irion.common.util.JsonResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/live")
public class LiveController {

    private static final Logger logger = LoggerFactory.getLogger(LiveController.class);

    private static final String CHANNEL_ID = "516937b5f85cbf2249ce31b0ad046b0f";
    private static final String CHZZK_API_URL = "https://api.chzzk.naver.com/service/v3/channels/" + CHANNEL_ID + "/live-detail";

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
            logger.debug("Chzzk API response code: {}", responseCode);

            if (responseCode == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                br.close();

                String jsonResponse = response.toString();
                logger.debug("Chzzk API response: {}", jsonResponse);

                Map<String, Object> data = new HashMap<>();

                boolean isLive = jsonResponse.contains("\"status\":\"OPEN\"");
                data.put("isLive", isLive);
                data.put("channelId", CHANNEL_ID);
                data.put("channelUrl", "https://chzzk.naver.com/live/" + CHANNEL_ID);

                if (isLive) {
                    // 방송 제목 추출
                    String liveTitle = extractJsonValue(jsonResponse, "liveTitle");
                    if (liveTitle != null) {
                        data.put("liveTitle", liveTitle);
                    }

                    // 썸네일 추출
                    String thumbnail = extractJsonValue(jsonResponse, "liveImageUrl");
                    if (thumbnail != null) {
                        // {type} 부분을 실제 크기로 변경
                        thumbnail = thumbnail.replace("{type}", "480");
                        data.put("thumbnail", thumbnail);
                    }

                    // 시청자 수 추출
                    String viewerCount = extractJsonNumber(jsonResponse, "concurrentUserCount");
                    if (viewerCount != null) {
                        data.put("viewerCount", viewerCount);
                    }
                }

                return JsonResult.success("조회 성공", data);
            } else {
                logger.warn("Chzzk API error: {}", responseCode);
                return JsonResult.fail("API 호출 실패");
            }

        } catch (Exception e) {
            logger.error("Error checking live status", e);
            return JsonResult.fail("방송 상태 확인 중 오류 발생");
        }
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
            while (endIndex < json.length() && (Character.isDigit(json.charAt(endIndex)) || json.charAt(endIndex) == '.')) {
                endIndex++;
            }

            if (endIndex == startIndex) return null;
            return json.substring(startIndex, endIndex);
        } catch (Exception e) {
            return null;
        }
    }

}
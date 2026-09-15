package com.irion.integration.chzzk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * 치지직 API 클라이언트. 호출·파싱만 담당, 캐시는 LiveFeedService.
 * 파싱은 반드시 ObjectMapper — 직접 자르면 제목에 큰따옴표가 든 항목이 깨진다
 */
@Component
public class ChzzkClient {

    private static final Logger logger = LoggerFactory.getLogger(ChzzkClient.class);

    /** 실패 로그 간격. 매번 남기면 로그가 쓰레기가 됨 — 클립은 한 요청에 최대 10회 호출 */
    private static final long LOG_INTERVAL_MILLIS = 60 * 1000L;

    private final AtomicLong lastLoggedAt = new AtomicLong();

    public static final String CHANNEL_ID = "63368ec9081dc85e61d0e4310b7e1602";

    private static final String LIVE_DETAIL_API =
            "https://api.chzzk.naver.com/service/v3/channels/" + CHANNEL_ID + "/live-detail";

    /** size 50 초과 시 400 */
    public static final int CLIP_PAGE_SIZE = 50;
    public static final int VIDEO_PAGE_SIZE = 50;

    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 5000;

    /** ObjectMapper 는 스레드 안전 — 하나만 공유 */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 클립 한 페이지 + 다음 커서. offset 이 아니라 clipUID·readCount 방식 */
    public static final class ClipPage {
        private final List<Map<String, Object>> clips;
        private final String nextClipUID;
        private final String nextReadCount;

        ClipPage(List<Map<String, Object>> clips, String nextClipUID, String nextReadCount) {
            this.clips = clips;
            this.nextClipUID = nextClipUID;
            this.nextReadCount = nextReadCount;
        }

        public List<Map<String, Object>> getClips() {
            return clips;
        }

        public String getNextClipUID() {
            return nextClipUID;
        }

        public String getNextReadCount() {
            return nextReadCount;
        }
    }

    /** 방송 상태. 실패 시 null — 호출부는 만료 캐시로 폴백 */
    public Map<String, Object> fetchLiveStatus() {
        JsonNode root = fetchApi(LIVE_DETAIL_API);
        if (root == null)
            return null;

        boolean isLive = false;
        for (JsonNode status : root.findValues("status")) {
            if ("OPEN".equals(status.asText())) {
                isLive = true;
                break;
            }
        }

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("isLive", isLive);
        data.put("channelId", CHANNEL_ID);
        data.put("channelUrl", "https://chzzk.naver.com/live/" + CHANNEL_ID);

        if (isLive) {
            data.put("liveTitle", text(root, "liveTitle"));
            // 19금은 liveImageUrl 이 null — 대체 자리를 그리도록 adult 도 전달
            data.put("thumbnail", text(root, "liveImageUrl").replace("{type}", "480"));
            data.put("adult", bool(root, "adult"));
            data.put("viewerCount", number(root, "concurrentUserCount"));
        }

        return data;
    }

    /** 인기순 클립 한 페이지. clipUID 없으면 첫 페이지, 실패 시 null */
    public ClipPage fetchClipPage(String clipUID, String readCount) {
        StringBuilder apiUrl = new StringBuilder()
                .append("https://api.chzzk.naver.com/service/v1/channels/").append(CHANNEL_ID)
                .append("/clips?filterType=ALL&orderType=POPULAR&size=").append(CLIP_PAGE_SIZE);

        if (clipUID != null && !clipUID.isEmpty()) {
            apiUrl.append("&clipUID=").append(clipUID);
            if (readCount != null && !readCount.isEmpty())
                apiUrl.append("&readCount=").append(readCount);
        }

        JsonNode root = fetchApi(apiUrl.toString());
        if (root == null)
            return null;

        String[] cursor = extractNextCursor(root);
        return new ClipPage(
                parseArray(root, this::parseClip),
                cursor != null ? cursor[0] : null,
                cursor != null ? cursor[1] : null);
    }

    /** 최신순 다시보기 한 페이지. 실패 시 null */
    public List<Map<String, Object>> fetchVideoPage(int page) {
        String apiUrl = "https://api.chzzk.naver.com/service/v1/channels/" + CHANNEL_ID
                + "/videos?sortType=LATEST&pagingType=PAGE&page=" + page + "&size=" + VIDEO_PAGE_SIZE;

        JsonNode root = fetchApi(apiUrl);
        return (root == null) ? null : parseArray(root, this::parseVideo);
    }

    // 테스트에서 응답 조각을 직접 넣으므로 package-private
    Map<String, Object> parseClip(JsonNode json) {
        String clipUID = text(json, "clipUID");
        if (clipUID.isEmpty())
            return null;

        Map<String, Object> clip = new HashMap<String, Object>();
        clip.put("clipId", clipUID);
        clip.put("clipTitle", text(json, "clipTitle"));
        clip.put("thumbnailUrl", text(json, "thumbnailImageUrl"));
        clip.put("adult", bool(json, "adult"));
        clip.put("viewCount", number(json, "readCount"));
        clip.put("duration", number(json, "duration"));
        clip.put("createdAt", text(json, "createdDate"));
        clip.put("clipUrl", "https://chzzk.naver.com/clips/" + clipUID);
        return clip;
    }

    Map<String, Object> parseVideo(JsonNode json) {
        String videoNo = number(json, "videoNo");
        if (videoNo.isEmpty())
            return null;

        Map<String, Object> video = new HashMap<String, Object>();
        video.put("videoNo", videoNo);
        video.put("videoTitle", text(json, "videoTitle"));
        video.put("thumbnailUrl", text(json, "thumbnailImageUrl"));
        video.put("adult", bool(json, "adult"));
        video.put("duration", number(json, "duration"));
        video.put("readCount", number(json, "readCount"));
        video.put("publishDate", text(json, "publishDate"));
        video.put("videoUrl", "https://chzzk.naver.com/video/" + videoNo);
        return video;
    }

    /** 엔드포인트마다 응답 깊이가 달라 경로 대신 이름으로 탐색 */
    List<Map<String, Object>> parseArray(JsonNode root, Function<JsonNode, Map<String, Object>> parser) {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

        JsonNode data = root.findValue("data");
        if (data == null || !data.isArray())
            return list;

        for (JsonNode item : data) {
            Map<String, Object> parsed = parser.apply(item);
            if (parsed != null) {
                list.add(parsed);
            }
        }

        return list;
    }

    /** 다음 페이지 커서 — clipUID · readCount 둘 다 필요 */
    private String[] extractNextCursor(JsonNode root) {
        JsonNode next = root.findValue("next");
        if (next == null || next.isNull())
            return null;

        String uid = text(next, "clipUID");
        if (uid.isEmpty())
            return null;

        return new String[] { uid, number(next, "readCount") };
    }

    /** 이름으로 문자열 조회. 없으면 빈 문자열(호출부의 null 처리 제거) */
    String text(JsonNode node, String key) {
        JsonNode found = node.findValue(key);
        return (found == null || found.isNull()) ? "" : found.asText();
    }

    /** 이름으로 숫자 조회. 화면·중복 판정이 문자열을 기대하므로 문자열로 */
    String number(JsonNode node, String key) {
        JsonNode found = node.findValue(key);
        return (found == null || !found.isNumber()) ? "" : found.asText();
    }

    /** 이름으로 불리언 조회. 없으면 false — 모르면 제한 없음 */
    boolean bool(JsonNode node, String key) {
        JsonNode found = node.findValue(key);
        return found != null && found.isBoolean() && found.asBoolean();
    }

    /** API 호출, 실패 시 null. 스트림을 비워야 연결이 풀로 반환됨 */
    private JsonNode fetchApi(String apiUrl) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(apiUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);

            int status = conn.getResponseCode();
            if (status != 200) {
                drain(conn.getErrorStream());
                // 403·429 는 우리가 막힌 것, 5xx 는 저쪽 사정 — 상태 코드가 곧 원인
                logFailure(apiUrl, "HTTP " + status);
                return null;
            }

            return MAPPER.readTree(readAll(conn.getInputStream()));

        } catch (Exception e) {
            if (conn != null) {
                conn.disconnect();
            }
            // 타임아웃인지 응답 형식이 바뀐 것인지가 여기서 갈림
            logFailure(apiUrl, e.getClass().getSimpleName()
                    + (e.getMessage() != null ? ": " + e.getMessage() : ""));
            return null;
        }
    }

    /**
     * 실패 원인 기록. 없으면 응답 형식이 바뀌거나 차단돼도
     * 화면은 낡은 캐시로 멀쩡해 보여 알아챌 수 없다. 한 번 막히면 계속이므로 간격을 둔다
     */
    private void logFailure(String apiUrl, String reason) {
        long now = System.currentTimeMillis();
        long last = lastLoggedAt.get();

        if (now - last < LOG_INTERVAL_MILLIS || !lastLoggedAt.compareAndSet(last, now)) {
            return;
        }

        int query = apiUrl.indexOf('?');
        String endpoint = (query < 0) ? apiUrl : apiUrl.substring(0, query);

        logger.warn("치지직 호출 실패: {} — {} (같은 로그는 {}초간 생략합니다)",
                endpoint, reason, LOG_INTERVAL_MILLIS / 1000);
    }

    /** 다 읽고 닫아야 연결이 풀로 반환됨 */
    private static String readAll(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    /** 내용은 안 쓰지만 비워야 연결이 풀로 반환됨 */
    private static void drain(InputStream in) {
        if (in == null) {
            return;
        }
        try {
            readAll(in);
        } catch (IOException e) {
            // 비우기만 하던 것이라 실패해도 할 일 없음
        }
    }

    /** 테스트용 — 응답 본문 직접 주입 */
    static JsonNode parse(String json) throws java.io.IOException {
        return MAPPER.readTree(json);
    }
}

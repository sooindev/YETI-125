package com.irion.common.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.function.Function;

/**
 * 치지직 공개 API 클라이언트. 호출과 파싱만 담당하고 캐시는 LiveFeedService 가 맡는다.
 * 파싱은 반드시 ObjectMapper 로 — 직접 자르면 제목에 큰따옴표가 든 항목이 깨진다.
 */
@Component
public class ChzzkClient {

    public static final String CHANNEL_ID = "63368ec9081dc85e61d0e4310b7e1602";

    private static final String LIVE_DETAIL_API =
            "https://api.chzzk.naver.com/service/v3/channels/" + CHANNEL_ID + "/live-detail";

    /** chzzk 는 size 가 50 을 넘으면 400 을 준다 */
    public static final int CLIP_PAGE_SIZE = 50;
    public static final int VIDEO_PAGE_SIZE = 50;

    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 5000;

    /** ObjectMapper 는 스레드 안전하므로 하나만 두고 공유한다 */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 클립 한 페이지와 다음 커서. offset 이 아니라 clipUID·readCount 를 되돌려주는 방식이다. */
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

    /** 방송 상태. 실패하면 null — 호출부는 만료된 캐시로 물러난다 */
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
            // 19금 방송은 liveImageUrl 이 null 로 온다 — 화면이 대체 자리를 그릴 수 있게 adult 도 함께 넘긴다
            data.put("thumbnail", text(root, "liveImageUrl").replace("{type}", "480"));
            data.put("adult", bool(root, "adult"));
            data.put("viewerCount", number(root, "concurrentUserCount"));
        }

        return data;
    }

    /** 인기순 클립 한 페이지. clipUID 가 비어 있으면 첫 페이지. 실패하면 null */
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

    /** 최신순 다시보기 한 페이지. 실패하면 null. */
    public List<Map<String, Object>> fetchVideoPage(int page) {
        String apiUrl = "https://api.chzzk.naver.com/service/v1/channels/" + CHANNEL_ID
                + "/videos?sortType=LATEST&pagingType=PAGE&page=" + page + "&size=" + VIDEO_PAGE_SIZE;

        JsonNode root = fetchApi(apiUrl);
        return (root == null) ? null : parseArray(root, this::parseVideo);
    }

    // 테스트에서 응답 조각을 직접 넣어보므로 package-private
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

    /** 엔드포인트마다 응답 깊이가 달라, 경로 대신 이름으로 data 배열을 찾는다 */
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

    /** 다음 페이지 커서 — clipUID 와 readCount 둘 다 있어야 한다 */
    private String[] extractNextCursor(JsonNode root) {
        JsonNode next = root.findValue("next");
        if (next == null || next.isNull())
            return null;

        String uid = text(next, "clipUID");
        if (uid.isEmpty())
            return null;

        return new String[] { uid, number(next, "readCount") };
    }

    /** 이름으로 문자열 찾기. 없으면 빈 문자열 (호출부가 null 을 안 다루도록) */
    String text(JsonNode node, String key) {
        JsonNode found = node.findValue(key);
        return (found == null || found.isNull()) ? "" : found.asText();
    }

    /** 이름으로 숫자 찾기. 화면과 중복 판정 키가 문자열을 기대하므로 문자열로 */
    String number(JsonNode node, String key) {
        JsonNode found = node.findValue(key);
        return (found == null || !found.isNumber()) ? "" : found.asText();
    }

    /** 이름으로 참/거짓 찾기. 없으면 false — 모르는 건 제한 없음으로 본다 */
    boolean bool(JsonNode node, String key) {
        JsonNode found = node.findValue(key);
        return found != null && found.isBoolean() && found.asBoolean();
    }

    /** API 호출, 실패하면 null. 스트림을 비워야 연결이 풀로 돌아간다. */
    private JsonNode fetchApi(String apiUrl) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(apiUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);

            if (conn.getResponseCode() != 200) {
                drain(conn.getErrorStream());
                return null;
            }

            return MAPPER.readTree(readAll(conn.getInputStream()));

        } catch (Exception e) {
            if (conn != null) {
                conn.disconnect();
            }
            return null;
        }
    }

    /** 다 읽고 닫아야 연결이 풀로 돌아간다 */
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

    /** 내용은 쓰지 않지만 비워야 연결이 풀로 돌아간다 */
    private static void drain(InputStream in) {
        if (in == null) {
            return;
        }
        try {
            readAll(in);
        } catch (IOException e) {
            // 비우려던 것뿐이라 실패해도 할 일이 없다
        }
    }

    /** 테스트에서 응답 본문을 직접 넣을 때 쓴다 */
    static JsonNode parse(String json) throws java.io.IOException {
        return MAPPER.readTree(json);
    }
}

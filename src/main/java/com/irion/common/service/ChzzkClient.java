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
 * 치지직 공개 API 클라이언트.
 *
 * 바깥으로 나가는 HTTP 호출과 응답 파싱만 담당한다. 캐시나 페이지네이션은
 * 여기서 다루지 않는다 (LiveFeedService).
 *
 * 응답 파싱은 ObjectMapper 로 한다. 예전에는 indexOf 로 문자열을 잘라
 * 썼는데 JSON 이스케이프를 몰라서, 제목에 큰따옴표가 들어가면
 *
 *   {"clipTitle":"이리온 \"레전드\" 순간"}  →  이리온 \
 *
 * 처럼 그 앞에서 잘렸다. \n, \\, \/ 도 디코드되지 않았다.
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

    /**
     * 클립 한 페이지와 다음 커서.
     *
     * chzzk 의 클립 페이징은 offset 이 아니라 커서다. 응답의 page.next 에
     * 담긴 clipUID 와 readCount 를 다음 요청에 같은 이름의 파라미터로
     * 되돌려줘야 그 다음 50개가 온다.
     */
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

    /**
     * 방송 상태.
     *
     * 호출이나 파싱에 실패하면 null 이다. 호출부는 그걸 보고 만료된
     * 캐시로 물러난다.
     */
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
            data.put("thumbnail", text(root, "liveImageUrl").replace("{type}", "480"));
            data.put("viewerCount", number(root, "concurrentUserCount"));
        }

        return data;
    }

    /**
     * 인기순 클립 한 페이지.
     *
     * clipUID 가 비어 있으면 첫 페이지다. 실패하면 null.
     */
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

    // ========================================
    // 파싱
    // ========================================

    // 테스트에서 응답 조각을 직접 넣어보므로 package-private
    Map<String, Object> parseClip(JsonNode json) {
        String clipUID = text(json, "clipUID");
        if (clipUID.isEmpty())
            return null;

        Map<String, Object> clip = new HashMap<String, Object>();
        clip.put("clipId", clipUID);
        clip.put("clipTitle", text(json, "clipTitle"));
        clip.put("thumbnailUrl", text(json, "thumbnailImageUrl"));
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
        video.put("duration", number(json, "duration"));
        video.put("readCount", number(json, "readCount"));
        video.put("publishDate", text(json, "publishDate"));
        video.put("videoUrl", "https://chzzk.naver.com/video/" + videoNo);
        return video;
    }

    /**
     * 응답에서 data 배열을 찾아 항목별로 파서를 돌린다.
     *
     * 클립과 다시보기의 응답 모양이 조금씩 달라 content 아래 깊이가
     * 일정하지 않다. 이름으로 찾아 첫 배열을 쓴다.
     */
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

    /**
     * 다음 페이지 커서 — {clipUID, readCount} 두 값이 모두 필요하다.
     * 둘 다 같은 이름의 쿼리 파라미터로 되돌려줘야 다음 묶음이 온다.
     */
    private String[] extractNextCursor(JsonNode root) {
        JsonNode next = root.findValue("next");
        if (next == null || next.isNull())
            return null;

        String uid = text(next, "clipUID");
        if (uid.isEmpty())
            return null;

        return new String[] { uid, number(next, "readCount") };
    }

    /**
     * 이름으로 문자열 값 찾기.
     *
     * 응답 깊이가 엔드포인트마다 달라 경로를 못 박지 않고 이름으로 찾는다.
     * 값이 없으면 빈 문자열이다 (호출부가 null 을 다루지 않아도 되도록).
     */
    String text(JsonNode node, String key) {
        JsonNode found = node.findValue(key);
        return (found == null || found.isNull()) ? "" : found.asText();
    }

    /**
     * 이름으로 숫자 값 찾기.
     *
     * 화면과 중복 판정 키가 문자열을 기대하므로 문자열로 돌려준다.
     * 숫자가 아니면 빈 문자열이다.
     */
    String number(JsonNode node, String key) {
        JsonNode found = node.findValue(key);
        return (found == null || !found.isNumber()) ? "" : found.asText();
    }

    // ========================================
    // HTTP
    // ========================================

    /**
     * API 호출 — 응답을 파싱한 트리로 돌려준다. 실패하면 null.
     *
     * 200 이 아닐 때 그냥 돌아서면 안 된다. HttpURLConnection 은 응답 본문을
     * 끝까지 읽고 스트림을 닫아야 그 연결을 keep-alive 풀에 돌려준다.
     * 오류 응답의 본문(errorStream)을 버려두면 연결이 풀로 돌아가지 못하고,
     * 치지직 쪽 장애가 길게 이어지면 그런 연결이 계속 쌓인다.
     *
     * disconnect() 는 예외로 끝났을 때만 부른다. 정상 경로에서 부르면
     * 소켓을 끊어버려 재사용 자체를 막는다.
     */
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

    /** 스트림을 끝까지 읽어 문자열로. 다 읽고 닫아야 연결이 풀로 돌아간다. */
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

    /** 오류 응답 본문 — 내용은 쓰지 않지만, 비워야 연결이 풀로 돌아간다 */
    private static void drain(InputStream in) {
        if (in == null) {
            return;
        }
        try {
            readAll(in);
        } catch (IOException e) {
            // 비우려던 것뿐이다. 실패해도 더 할 일이 없다.
        }
    }

    /** 문자열 JSON 파싱 — 테스트에서 응답 본문을 직접 넣을 때 쓴다 */
    static JsonNode parse(String json) throws java.io.IOException {
        return MAPPER.readTree(json);
    }
}

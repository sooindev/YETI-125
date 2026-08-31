package com.irion.common.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/** 파싱 회귀 테스트. 옛 indexOf 구현은 제목에 큰따옴표가 들어가면 그 앞에서 잘렸다. */
public class ChzzkClientTest {

    private final ChzzkClient client = new ChzzkClient();

    @Test
    public void 제목에_큰따옴표가_있어도_끝까지_읽는다() throws Exception {
        // 옛 구현은 여기서 [이리온 \] 를 돌려줬다
        JsonNode node = ChzzkClient.parse(
                "{\"clipUID\":\"abc\",\"clipTitle\":\"이리온 \\\"레전드\\\" 순간\"}");

        assertEquals("이리온 \"레전드\" 순간", client.text(node, "clipTitle"));
    }

    @Test
    public void 이스케이프_문자를_디코드한다() throws Exception {
        JsonNode node = ChzzkClient.parse(
                "{\"clipTitle\":\"줄바꿈\\n역슬래시\\\\슬래시\\/탭\\t\"}");

        assertEquals("줄바꿈\n역슬래시\\슬래시/탭\t", client.text(node, "clipTitle"));
    }

    @Test
    public void 유니코드_이스케이프를_디코드한다() throws Exception {
        JsonNode node = ChzzkClient.parse("{\"clipTitle\":\"\\uc774\\ub9ac\\uc628\"}");

        assertEquals("이리온", client.text(node, "clipTitle"));
    }

    @Test
    public void 없는_키는_빈_문자열이다() throws Exception {
        JsonNode node = ChzzkClient.parse("{\"clipUID\":\"abc\"}");

        assertEquals("", client.text(node, "clipTitle"));
        assertEquals("", client.number(node, "readCount"));
    }

    @Test
    public void null_값은_빈_문자열이다() throws Exception {
        JsonNode node = ChzzkClient.parse("{\"clipTitle\":null,\"readCount\":null}");

        assertEquals("", client.text(node, "clipTitle"));
        assertEquals("", client.number(node, "readCount"));
    }

    @Test
    public void 숫자가_아닌_값은_숫자로_읽지_않는다() throws Exception {
        JsonNode node = ChzzkClient.parse("{\"readCount\":\"1234\"}");

        assertEquals("", client.number(node, "readCount"));
    }

    @Test
    public void 클립_한_건을_통째로_파싱한다() throws Exception {
        JsonNode node = ChzzkClient.parse("{"
                + "\"clipUID\":\"CLIP-1\","
                + "\"clipTitle\":\"제목에 \\\"따옴표\\\" 와 <태그>\","
                + "\"thumbnailImageUrl\":\"https://cdn.example/thumb.jpg\","
                + "\"readCount\":4321,"
                + "\"duration\":75,"
                + "\"createdDate\":\"2024-02-29 12:00:00\"}");

        Map<String, Object> clip = client.parseClip(node);

        assertNotNull(clip);
        assertEquals("CLIP-1", clip.get("clipId"));
        assertEquals("제목에 \"따옴표\" 와 <태그>", clip.get("clipTitle"));
        assertEquals("https://cdn.example/thumb.jpg", clip.get("thumbnailUrl"));
        assertEquals("4321", clip.get("viewCount"));
        assertEquals("75", clip.get("duration"));
        assertEquals("2024-02-29 12:00:00", clip.get("createdAt"));
        assertEquals("https://chzzk.naver.com/clips/CLIP-1", clip.get("clipUrl"));
    }

    @Test
    public void clipUID_가_없으면_건너뛴다() throws Exception {
        assertNull(client.parseClip(ChzzkClient.parse("{\"clipTitle\":\"제목뿐\"}")));
    }

    /** 19금은 썸네일이 null 로 온다. 화면이 대체 자리를 그리려면 adult 가 함께 와야 한다 */
    @Test
    public void 연령제한_클립은_썸네일이_비고_adult_가_참이다() throws Exception {
        JsonNode node = ChzzkClient.parse("{"
                + "\"clipUID\":\"CLIP-19\","
                + "\"clipTitle\":\"19금 클립\","
                + "\"thumbnailImageUrl\":null,"
                + "\"adult\":true}");

        Map<String, Object> clip = client.parseClip(node);

        assertEquals("", clip.get("thumbnailUrl"));
        assertEquals(Boolean.TRUE, clip.get("adult"));
    }

    @Test
    public void 다시보기_한_건을_통째로_파싱한다() throws Exception {
        JsonNode node = ChzzkClient.parse("{"
                + "\"videoNo\":319019,"
                + "\"videoTitle\":\"이리온의 재채기.mp4\","
                + "\"thumbnailImageUrl\":\"https://cdn.example/v.jpg\","
                + "\"duration\":3600,"
                + "\"readCount\":10,"
                + "\"publishDate\":\"2024-02-29 12:00:00\"}");

        Map<String, Object> video = client.parseVideo(node);

        assertNotNull(video);
        assertEquals("319019", video.get("videoNo"));
        assertEquals("이리온의 재채기.mp4", video.get("videoTitle"));
        assertEquals("https://chzzk.naver.com/video/319019", video.get("videoUrl"));
    }

    @Test
    public void videoNo_가_없으면_건너뛴다() throws Exception {
        assertNull(client.parseVideo(ChzzkClient.parse("{\"videoTitle\":\"제목뿐\"}")));
    }

    @Test
    public void 연령제한_다시보기는_썸네일이_비고_adult_가_참이다() throws Exception {
        JsonNode node = ChzzkClient.parse("{"
                + "\"videoNo\":14931235,"
                + "\"videoTitle\":\"주말 아침을 나랑 + 19금\","
                + "\"thumbnailImageUrl\":null,"
                + "\"adult\":true}");

        Map<String, Object> video = client.parseVideo(node);

        assertEquals("", video.get("thumbnailUrl"));
        assertEquals(Boolean.TRUE, video.get("adult"));
    }

    /** adult 가 아예 없거나 참/거짓이 아니면 제한 없음으로 본다 */
    @Test
    public void adult_가_없으면_제한_없음이다() throws Exception {
        assertEquals(Boolean.FALSE,
                client.parseVideo(ChzzkClient.parse("{\"videoNo\":1}")).get("adult"));
        assertFalse(client.bool(ChzzkClient.parse("{\"adult\":\"true\"}"), "adult"));
    }

    @Test
    public void data_배열의_항목을_순서대로_읽는다() throws Exception {
        JsonNode root = ChzzkClient.parse("{\"code\":200,\"content\":{\"data\":["
                + "{\"clipUID\":\"a\",\"clipTitle\":\"첫 번째\"},"
                + "{\"clipUID\":\"b\",\"clipTitle\":\"두 번째 \\\"인용\\\"\"},"
                + "{\"clipTitle\":\"UID 없음 — 버려진다\"}"
                + "]}}");

        List<Map<String, Object>> clips = client.parseArray(root, client::parseClip);

        assertEquals(2, clips.size());
        assertEquals("첫 번째", clips.get(0).get("clipTitle"));
        assertEquals("두 번째 \"인용\"", clips.get(1).get("clipTitle"));
    }

    @Test
    public void data_가_없으면_빈_목록이다() throws Exception {
        JsonNode root = ChzzkClient.parse("{\"code\":500,\"content\":null}");

        assertTrue(client.parseArray(root, client::parseClip).isEmpty());
    }

    @Test
    public void 중괄호가_섞인_제목에도_배열_경계를_잃지_않는다() throws Exception {
        // 옛 findBracket 구현이 흔들리던 모양
        JsonNode root = ChzzkClient.parse("{\"content\":{\"data\":["
                + "{\"clipUID\":\"a\",\"clipTitle\":\"{ 중괄호 } 와 [ 대괄호 ]\"},"
                + "{\"clipUID\":\"b\",\"clipTitle\":\"끝\"}"
                + "]}}");

        List<Map<String, Object>> clips = client.parseArray(root, client::parseClip);

        assertEquals(2, clips.size());
        assertEquals("{ 중괄호 } 와 [ 대괄호 ]", clips.get(0).get("clipTitle"));
    }
}

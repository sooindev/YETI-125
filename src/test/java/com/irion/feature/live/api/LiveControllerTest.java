package com.irion.feature.live.api;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * 목록 API 가 값을 다듬는 규칙 — paginate 의 경계값과, 클립 아카이브가 쓰는 정렬·검색.
 */
public class LiveControllerTest {

    private final LiveController controller = new LiveController();

    // ── 정렬·검색 (클립 아카이브) ────────────────────────

    @Test
    public void 최신순은_만든_날_내림차순이다() {
        List<Map<String, Object>> sorted = controller.arrange(
                clips("2024-01-01 10:00:00", "2026-03-05 09:00:00", "2025-07-30 21:54:50"),
                "latest", "");

        assertEquals("2026-03-05 09:00:00", sorted.get(0).get("createdAt"));
        assertEquals("2025-07-30 21:54:50", sorted.get(1).get("createdAt"));
        assertEquals("2024-01-01 10:00:00", sorted.get(2).get("createdAt"));
    }

    /** 날짜를 못 받은 항목이 맨 앞으로 올라오면 최신순이 거짓말이 된다 */
    @Test
    public void 날짜가_없는_클립은_뒤로_밀린다() {
        List<Map<String, Object>> withBlank = clips("2024-01-01 10:00:00", "");

        List<Map<String, Object>> sorted = controller.arrange(withBlank, "latest", "");

        assertEquals("2024-01-01 10:00:00", sorted.get(0).get("createdAt"));
        assertEquals("", sorted.get(1).get("createdAt"));
    }

    @Test
    public void 정렬을_지정하지_않으면_받은_차례_그대로다() {
        List<Map<String, Object>> given = clips("2024-01-01 10:00:00", "2026-03-05 09:00:00");

        List<Map<String, Object>> kept = controller.arrange(given, null, "");

        assertEquals("2024-01-01 10:00:00", kept.get(0).get("createdAt"));
    }

    /**
     * 이 테스트가 있는 이유. 캐시가 들고 있는 목록을 제자리에서 정렬하면
     * 다음 요청이 뒤섞인 차례를 "인기순"이라고 믿는다.
     */
    @Test
    public void 정렬해도_원본_목록은_그대로다() {
        List<Map<String, Object>> original = clips("2024-01-01 10:00:00", "2026-03-05 09:00:00");

        controller.arrange(original, "latest", "");

        assertEquals("원본이 뒤집히면 안 된다", "2024-01-01 10:00:00", original.get(0).get("createdAt"));
    }

    @Test
    public void 제목에_들어_있는_말로_거른다() {
        List<Map<String, Object>> found = controller.arrange(titled("노래방송 하이라이트", "게임 실수"), null, "노래");

        assertEquals(1, found.size());
        assertEquals("노래방송 하이라이트", found.get(0).get("clipTitle"));
    }

    @Test
    public void 검색은_영문_대소문자를_가리지_않는다() {
        List<Map<String, Object>> found = controller.arrange(titled("IRION Clip", "다른 것"), null, "irion");

        assertEquals(1, found.size());
    }

    @Test
    public void 검색어가_비면_전부_남는다() {
        assertEquals(2, controller.arrange(titled("가", "나"), null, "").size());
    }

    @Test
    public void 아무것도_맞지_않으면_빈_목록이다() {
        assertTrue(controller.arrange(titled("가", "나"), null, "없는말").isEmpty());
    }

    @Test
    public void 제목이_없는_클립은_검색에서_조용히_빠진다() {
        List<Map<String, Object>> withNull = new ArrayList<Map<String, Object>>();
        withNull.add(new HashMap<String, Object>());   // clipTitle 이 없다

        assertTrue(controller.arrange(withNull, null, "가").isEmpty());
    }

    @Test
    public void 빈_목록과_null_목록도_견딘다() {
        assertTrue(controller.arrange(null, "latest", "가").isEmpty());
        assertTrue(controller.arrange(new ArrayList<Map<String, Object>>(), "latest", "").isEmpty());
    }

    private static List<Map<String, Object>> clips(String... createdAt) {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (String at : createdAt) {
            Map<String, Object> clip = new HashMap<String, Object>();
            clip.put("createdAt", at);
            list.add(clip);
        }
        return list;
    }

    private static List<Map<String, Object>> titled(String... titles) {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (String title : titles) {
            Map<String, Object> clip = new HashMap<String, Object>();
            clip.put("clipTitle", title);
            list.add(clip);
        }
        return list;
    }

    @Test
    public void 첫_페이지를_자른다() {
        Map<String, Object> page = controller.paginate(items(10), "clips", 0, 6);

        assertEquals(6, list(page, "clips").size());
        assertEquals("item-0", list(page, "clips").get(0).get("id"));
        assertEquals(Boolean.TRUE, page.get("hasMore"));
        assertEquals(6, page.get("nextOffset"));
    }

    @Test
    public void 마지막_페이지에서는_hasMore_가_꺼진다() {
        Map<String, Object> page = controller.paginate(items(10), "clips", 6, 6);

        assertEquals(4, list(page, "clips").size());
        assertEquals(Boolean.FALSE, page.get("hasMore"));
        assertEquals(10, page.get("nextOffset"));
    }

    @Test
    public void 딱_맞게_끝나면_hasMore_가_꺼진다() {
        Map<String, Object> page = controller.paginate(items(12), "clips", 6, 6);

        assertEquals(6, list(page, "clips").size());
        assertEquals(Boolean.FALSE, page.get("hasMore"));
    }

    @Test
    public void offset_이_목록을_넘어도_예외가_아니라_빈_페이지다() {
        // subList 가 IndexOutOfBounds 를 던지던 자리
        Map<String, Object> page = controller.paginate(items(10), "clips", 999, 6);

        assertTrue(list(page, "clips").isEmpty());
        assertEquals(Boolean.FALSE, page.get("hasMore"));
        assertEquals(10, page.get("nextOffset"));
    }

    @Test
    public void offset_이_정확히_끝일_때도_빈_페이지다() {
        Map<String, Object> page = controller.paginate(items(10), "clips", 10, 6);

        assertTrue(list(page, "clips").isEmpty());
        assertEquals(Boolean.FALSE, page.get("hasMore"));
    }

    @Test
    public void 음수_offset_은_처음으로_본다() {
        Map<String, Object> page = controller.paginate(items(10), "clips", -5, 3);

        assertEquals(3, list(page, "clips").size());
        assertEquals("item-0", list(page, "clips").get(0).get("id"));
    }

    @Test
    public void 음수_limit_은_빈_페이지다() {
        Map<String, Object> page = controller.paginate(items(10), "clips", 0, -1);

        assertTrue(list(page, "clips").isEmpty());
        assertEquals(Boolean.TRUE, page.get("hasMore"));
    }

    @Test
    public void limit_이_목록보다_커도_있는_만큼만_준다() {
        Map<String, Object> page = controller.paginate(items(3), "clips", 0, 1000);

        assertEquals(3, list(page, "clips").size());
        assertEquals(Boolean.FALSE, page.get("hasMore"));
    }

    @Test
    public void 빈_목록과_null_목록() {
        Map<String, Object> empty = controller.paginate(
                Collections.<Map<String, Object>>emptyList(), "videos", 0, 6);
        assertTrue(list(empty, "videos").isEmpty());
        assertEquals(Boolean.FALSE, empty.get("hasMore"));

        Map<String, Object> none = controller.paginate(null, "videos", 0, 6);
        assertTrue(list(none, "videos").isEmpty());
        assertEquals(Boolean.FALSE, none.get("hasMore"));
    }

    @Test
    public void 잘라낸_목록은_원본과_분리된다() {
        List<Map<String, Object>> source = items(10);
        Map<String, Object> page = controller.paginate(source, "clips", 0, 3);

        source.clear();

        assertEquals("원본을 비워도 응답이 흔들리면 안 된다", 3, list(page, "clips").size());
    }


    private static List<Map<String, Object>> items(int count) {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < count; i++) {
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("id", "item-" + i);
            list.add(item);
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> list(Map<String, Object> page, String key) {
        return (List<Map<String, Object>>) page.get(key);
    }
}

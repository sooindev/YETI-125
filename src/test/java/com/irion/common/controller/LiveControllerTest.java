package com.irion.common.controller;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/** paginate 경계값 */
public class LiveControllerTest {

    private final LiveController controller = new LiveController();

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

    // ── 헬퍼 ─────────────────────────────────────

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

package com.irion.guestbook.controller;

import com.irion.common.util.JsonResult;
import com.irion.guestbook.service.GuestbookService;
import com.irion.guestbook.vo.GuestbookVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 데뷔 3주년 기념 방명록 — 누구나 읽고 쓸 수 있는 공개 화면.
 * 삭제는 관리자만 하므로 AdminGuestbookController 에 따로 있다 (/admin/* 이라야 인증 필터를 탄다).
 */
@Controller
@RequestMapping("/guestbook")
public class GuestbookController {

    /** 방명록은 전부 익명이다. 화면에 이 이름으로 서명된다 */
    public static final String ANONYMOUS = "익명";

    /**
     * 연달아 올릴 수 있는 간격. 세션 기준이라 쿠키를 지우면 풀린다 —
     * 새로고침 도배를 막는 정도고, 진짜 방어는 nginx 쪽 요청 제한이다.
     */
    private static final long WRITE_COOLDOWN_MILLIS = 30 * 1000L;

    private static final String LAST_WRITE_KEY = "guestbookLastWriteAt";

    @Autowired
    private GuestbookService guestbookService;

    @GetMapping("")
    public String guestbook() {
        return "guestbook";
    }

    /** 홈의 클립 목록과 같은 모양으로 돌려준다 — 화면 쪽 "더 보기" 처리를 하나로 맞춘다 */
    @GetMapping("/list")
    @ResponseBody
    public JsonResult getGuestbookList(@RequestParam(defaultValue = "0") int offset,
                                       @RequestParam(defaultValue = "12") int limit) {

        List<GuestbookVO> list = guestbookService.getGuestbookList(offset, limit);
        int total = guestbookService.getGuestbookCount();

        // 화면에 필요한 것만 골라 담는다. del_yn 은 내보낼 이유가 없다
        List<Map<String, Object>> entries = new ArrayList<>();
        for (GuestbookVO g : list) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("id", g.getGuestbookId());
            entry.put("nickname", g.getNickname());
            entry.put("content", g.getContent());
            entry.put("cheer", g.getCheer());
            entry.put("regDate", g.getRegDate());
            entries.add(entry);
        }

        int nextOffset = Math.max(offset, 0) + entries.size();

        Map<String, Object> data = new HashMap<>();
        data.put("entries", entries);
        data.put("total", total);
        data.put("nextOffset", nextOffset);
        data.put("hasMore", nextOffset < total);

        return JsonResult.success("조회되었습니다.", data);
    }

    @PostMapping("")
    @ResponseBody
    public JsonResult createGuestbook(@Valid @RequestBody GuestbookVO guestbookVO,
                                      BindingResult binding,
                                      HttpServletRequest request) {
        if (binding.hasErrors()) {
            return JsonResult.fail(firstMessage(binding));
        }

        HttpSession session = request.getSession();
        long remaining = cooldownRemaining(session);
        if (remaining > 0) {
            return JsonResult.fail(remaining + "초 후에 다시 남겨 주세요.");
        }

        // 글쓴이가 정하는 값이 아니다. 화면에서 입력칸을 없앤 것만으로는
        // API 로 직접 실어 보내는 것을 막지 못한다.
        guestbookVO.setNickname(ANONYMOUS);
        guestbookVO.setGuestbookId(null);
        guestbookVO.setDelYn(null);
        guestbookVO.setRegDate(null);

        Long guestbookId = guestbookService.createGuestbook(guestbookVO);

        if (guestbookId == null) {
            return JsonResult.fail("등록에 실패했습니다. 잠시 후 다시 시도해 주세요.");
        }

        session.setAttribute(LAST_WRITE_KEY, System.currentTimeMillis());

        return JsonResult.success("축하 메시지가 등록되었습니다. 고마워요!", guestbookId);
    }

    /** 다음 글까지 남은 초. 아직 쓸 수 있으면 0 */
    private long cooldownRemaining(HttpSession session) {
        Object lastWriteAt = session.getAttribute(LAST_WRITE_KEY);
        if (!(lastWriteAt instanceof Long)) {
            return 0;
        }

        long elapsed = System.currentTimeMillis() - (Long) lastWriteAt;
        if (elapsed >= WRITE_COOLDOWN_MILLIS) {
            return 0;
        }
        return (WRITE_COOLDOWN_MILLIS - elapsed + 999) / 1000;
    }

    /** 첫 검증 오류만 고른다. 여기서 안 막으면 길이 초과가 DB 제약에 걸려 500 이 난다. */
    private String firstMessage(BindingResult binding) {
        FieldError error = binding.getFieldError();
        if (error != null && error.getDefaultMessage() != null) {
            return error.getDefaultMessage();
        }
        return "입력값이 올바르지 않습니다.";
    }
}

package com.irion.feature.guestbook.api;

import com.irion.common.api.JsonResult;
import com.irion.feature.guestbook.service.GuestbookService;
import com.irion.feature.guestbook.domain.GuestbookVO;
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
 * 3주년 방명록 — 공개 읽기·쓰기.
 * 삭제는 AdminGuestbookController 에 따로 — /admin/* 이라야 인증 필터를 탄다
 */
@Controller
@RequestMapping("/guestbook")
public class GuestbookController {

    /** 전부 익명. 화면 서명에 쓰는 이름 */
    public static final String ANONYMOUS = "익명";

    /**
     * 연속 등록 간격. 세션 기준이라 쿠키를 지우면 풀린다 —
     * 새로고침 도배 방지용이고 진짜 방어는 nginx 요청 제한
     */
    private static final long WRITE_COOLDOWN_MILLIS = 30 * 1000L;

    private static final String LAST_WRITE_KEY = "guestbookLastWriteAt";

    @Autowired
    private GuestbookService guestbookService;

    @GetMapping("")
    public String guestbook() {
        return "pages/guestbook";
    }

    /** 홈 클립 목록과 같은 응답 모양 — 화면의 "더 보기" 처리를 통일 */
    @GetMapping("/list")
    @ResponseBody
    public JsonResult getGuestbookList(@RequestParam(defaultValue = "0") int offset,
                                       @RequestParam(defaultValue = "12") int limit) {

        List<GuestbookVO> list = guestbookService.getGuestbookList(offset, limit);
        int total = guestbookService.getGuestbookCount();

        // 화면에 필요한 것만. del_yn 은 내보낼 이유 없음
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

        // 글쓴이가 정하는 값이 아니다. 입력칸 제거만으로는 API 직접 호출을 못 막는다
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

    /** 다음 글까지 남은 초. 가능하면 0 */
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

    /** 첫 검증 오류만. 여기서 안 막으면 길이 초과가 DB 제약에 걸려 500 */
    private String firstMessage(BindingResult binding) {
        FieldError error = binding.getFieldError();
        if (error != null && error.getDefaultMessage() != null) {
            return error.getDefaultMessage();
        }
        return "입력값이 올바르지 않습니다.";
    }
}

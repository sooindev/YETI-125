package com.irion.guestbook.controller;

import com.irion.common.util.JsonResult;
import com.irion.guestbook.service.GuestbookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 방명록 삭제 — 관리자 전용.
 *
 * 주소가 /admin 아래인 것이 핵심이다. web.xml 이 /admin/* 에 로그인 필터와 CSRF 필터를 걸고
 * servlet-context.xml 이 인터셉터를 한 겹 더 얹는다. 공개 컨트롤러에 삭제를 두면
 * 그 세 겹을 전부 비켜 가므로, 화면에서 버튼을 숨기는 것만으로는 아무 방어가 되지 않는다.
 */
@Controller
@RequestMapping("/admin/guestbook")
public class AdminGuestbookController {

    @Autowired
    private GuestbookService guestbookService;

    @DeleteMapping("/{guestbookId}")
    @ResponseBody
    public JsonResult deleteGuestbook(@PathVariable Long guestbookId) {
        boolean success = guestbookService.deleteGuestbook(guestbookId);

        if (!success) {
            // 이미 지운 글을 다시 지르면 여기로 온다 — 없는 글도 같은 답을 준다
            return JsonResult.fail("삭제에 실패했습니다. 이미 지워진 글일 수 있습니다.");
        }

        return JsonResult.success("방명록을 삭제했습니다.");
    }
}

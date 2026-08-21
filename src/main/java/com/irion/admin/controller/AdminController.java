package com.irion.admin.controller;

import com.irion.admin.service.AdminService;
import com.irion.admin.vo.AdminVO;
import com.irion.common.util.CsrfTokens;
import com.irion.common.util.JsonResult;
import com.irion.common.util.LoginAttemptGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Collections;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private AdminService adminService;

    // 컨트롤러는 싱글턴이라 카운터도 하나만 있으면 된다
    private final LoginAttemptGuard loginGuard = new LoginAttemptGuard();

    // 로그인 페이지
    @GetMapping("/admin-login")
    public String loginPage(HttpSession session) {
        if (session.getAttribute("adminUser") != null) {
            return "redirect:/admin/admin-schedule.html";
        }
        return "redirect:/admin/admin-login.html";
    }

    /**
     * 로그인 처리 (AJAX)
     *
     * 세션을 injection 으로 받지 않고 요청에서 직접 꺼낸다. 인증에 성공하면
     * 쓰던 세션을 버리고 새로 발급해야 세션 고정 공격을 막을 수 있다.
     * (공격자가 미리 심어둔 세션 ID 로 로그인시키면, 그 ID 가 그대로
     *  관리자 세션이 되어버린다)
     */
    @PostMapping("/loginProc")
    @ResponseBody
    public JsonResult loginProc(@RequestParam String adminLoginId, @RequestParam String password,
                                HttpServletRequest request) {

        long lockedFor = loginGuard.lockedSecondsRemaining(adminLoginId);
        if (lockedFor > 0) {
            logger.warn("Admin login blocked (too many attempts): {}", mask(adminLoginId));
            return JsonResult.fail("로그인 시도가 너무 많습니다. "
                    + ((lockedFor + 59) / 60) + "분 후 다시 시도해 주세요.");
        }

        AdminVO admin = adminService.login(adminLoginId, password);

        if (admin == null) {
            loginGuard.recordFailure(adminLoginId);
            logger.warn("Admin login failed: {}", mask(adminLoginId));
            return JsonResult.fail("아이디 또는 비밀번호가 일치하지 않습니다.");
        }

        loginGuard.recordSuccess(adminLoginId);
        admin.setAdminPassword(null);

        // 세션 고정 방어 — 기존 세션을 버리고 새 ID 를 발급받는다
        HttpSession previous = request.getSession(false);
        if (previous != null) {
            previous.invalidate();
        }
        HttpSession session = request.getSession(true);
        session.setAttribute("adminUser", admin);
        CsrfTokens.reissue(session);

        logger.info("Admin login success: {}", mask(adminLoginId));
        return JsonResult.success("로그인 성공");
    }

    /**
     * CSRF 토큰 조회.
     *
     * 관리자 화면은 정적 HTML 이라 서버가 토큰을 심어줄 자리가 없다.
     * 로그인된 세션만 이 응답을 읽을 수 있고, 동일 출처 정책 때문에
     * 다른 사이트의 스크립트는 본문을 볼 수 없다.
     */
    @GetMapping("/csrf-token")
    @ResponseBody
    public Map<String, String> csrfToken(HttpSession session) {
        return Collections.singletonMap("token", CsrfTokens.issue(session));
    }

    /**
     * 로그아웃
     *
     * GET 이면 &lt;img src="/admin/logout"&gt; 하나로도 남의 세션을 끊을 수 있다.
     * POST 로 바꾸고 CsrfFilter 의 검사 대상에 들어가게 한다.
     */
    @PostMapping("/logout")
    @ResponseBody
    public JsonResult logout(HttpSession session) {
        AdminVO admin = (AdminVO) session.getAttribute("adminUser");
        if (admin != null) {
            logger.info("Admin logout: {}", mask(admin.getAdminLoginId()));
        }
        session.invalidate();
        return JsonResult.success("로그아웃되었습니다.");
    }

    // 관리자 메인
    @GetMapping("")
    public String adminMain() {
        return "redirect:/admin/admin-schedule.html";
    }

    /**
     * 로그인 ID 마스킹.
     *
     * 로그 파일에 계정 이름을 그대로 남기지 않는다. 어느 계정인지 뒤쫓을
     * 수 있을 만큼만 앞 두 글자를 남기고 가린다.
     */
    private static String mask(String loginId) {
        if (loginId == null || loginId.isEmpty()) {
            return "(none)";
        }
        if (loginId.length() <= 2) {
            return loginId.charAt(0) + "***";
        }
        return loginId.substring(0, 2) + "***";
    }

}

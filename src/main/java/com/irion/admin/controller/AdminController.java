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

    /** admin_login_id 의 컬럼 폭. 이보다 길면 어떤 계정과도 맞지 않는다 */
    private static final int MAX_LOGIN_ID_LENGTH = 50;

    // 컨트롤러가 싱글턴이라 카운터도 하나면 된다
    private final LoginAttemptGuard loginGuard = new LoginAttemptGuard();

    /**
     * 로그인 페이지.
     *
     * HttpSession 을 파라미터로 받으면 스프링이 없는 세션을 만들어 넣는다.
     * 누구나 열 수 있는 경로라 그것만으로 빈 세션이 쌓인다.
     */
    @GetMapping("/admin-login")
    public String loginPage(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("adminUser") != null) {
            return "redirect:/admin/admin-schedule.html";
        }
        return "redirect:/admin/admin-login.html";
    }

    /** 로그인 처리. 성공하면 세션을 새로 발급한다 (세션 고정 방어) */
    @PostMapping("/loginProc")
    @ResponseBody
    public JsonResult loginProc(@RequestParam String adminLoginId, @RequestParam String password,
                                HttpServletRequest request) {

        // 흘려보내면 DB 조회 파라미터로 들어가고 시도 카운터에도 자리를 차지한다.
        // 응답 문구는 일반 실패와 똑같이 둔다
        if (adminLoginId.length() > MAX_LOGIN_ID_LENGTH) {
            logger.warn("Admin login rejected (login id too long): {} chars", adminLoginId.length());
            return JsonResult.fail("아이디 또는 비밀번호가 일치하지 않습니다.");
        }

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

    /** 관리자 화면이 정적 HTML 이라 서버가 토큰을 심어줄 자리가 없어 따로 받아간다 */
    @GetMapping("/csrf-token")
    @ResponseBody
    public Map<String, String> csrfToken(HttpSession session) {
        return Collections.singletonMap("token", CsrfTokens.issue(session));
    }

    /** GET 이면 img 태그 하나로도 남의 세션을 끊을 수 있어 POST 로 둔다 */
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

    @GetMapping("")
    public String adminMain() {
        return "redirect:/admin/admin-schedule.html";
    }

    /** 로그에 계정 이름을 그대로 남기지 않는다. 뒤쫓을 만큼만 남기고 가린다 */
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

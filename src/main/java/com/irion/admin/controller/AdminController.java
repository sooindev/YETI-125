package com.irion.admin.controller;

import com.irion.admin.service.AdminService;
import com.irion.admin.vo.AdminVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private AdminService adminService;

    // 로그인 페이지
    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        // 이미 로그인된 경우
        if (session.getAttribute("adminUser") != null) {
            return "redirect:/admin/schedule";
        }
        return "admin/login";
    }

    // 로그인 처리
    @PostMapping("/loginProc")
    public String loginProc(@RequestParam String adminLoginId,
                            @RequestParam String password,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {

        logger.debug("Login attempt: {}", adminLoginId);

        AdminVO admin = adminService.login(adminLoginId, password);

        if (admin != null) {
            // 세션에 관리자 정보 저장 (비밀번호 제외)
            admin.setAdminPassword(null);
            session.setAttribute("adminUser", admin);

            logger.info("Admin login success: {}", adminLoginId);
            return "redirect:/admin/schedule";
        }

        logger.warn("Admin login failed: {}", adminLoginId);
        redirectAttributes.addFlashAttribute("errorMsg", "아이디 또는 비밀번호가 일치하지 않습니다.");
        return "redirect:/admin/login";
    }

    // 로그아웃
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        AdminVO admin = (AdminVO) session.getAttribute("adminUser");
        if (admin != null) {
            logger.info("Admin logout: {}", admin.getAdminLoginId());
        }
        session.invalidate();
        return "redirect:/admin/login";
    }

    // 관리자 메인 (일정 관리로 리다이렉트)
    @GetMapping("")
    public String adminMain() {
        return "redirect:/admin/schedule";
    }

}
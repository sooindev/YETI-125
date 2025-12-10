package com.irion.admin.controller;

import com.irion.admin.service.AdminService;
import com.irion.admin.vo.AdminVO;
import com.irion.common.util.JsonResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

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
        if (session.getAttribute("adminUser") != null) {
            return "redirect:/admin/admin-schedule.html";
        }
        return "redirect:/admin/login.html";
    }

    // 로그인 처리 (AJAX)
    @PostMapping("/loginProc")
    @ResponseBody
    public JsonResult loginProc(@RequestParam String adminLoginId, @RequestParam String password, HttpSession session) {

        logger.debug("Login attempt: {}", adminLoginId);

        AdminVO admin = adminService.login(adminLoginId, password);

        if (admin != null) {
            admin.setAdminPassword(null);
            session.setAttribute("adminUser", admin);
            logger.info("Admin login success: {}", adminLoginId);
            return JsonResult.success("로그인 성공");
        }

        logger.warn("Admin login failed: {}", adminLoginId);
        return JsonResult.fail("아이디 또는 비밀번호가 일치하지 않습니다.");
    }

    // 로그아웃
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        AdminVO admin = (AdminVO) session.getAttribute("adminUser");
        if (admin != null) {
            logger.info("Admin logout: {}", admin.getAdminLoginId());
        }
        session.invalidate();
        return "redirect:/admin/admin-login.html";
    }

    // 관리자 메인
    @GetMapping("")
    public String adminMain() {
        return "redirect:/admin/admin-schedule.html";
    }

}
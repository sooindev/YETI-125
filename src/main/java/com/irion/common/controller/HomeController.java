package com.irion.common.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    // 메인 페이지
    @GetMapping("/")
    public String home() {
        return "index";
    }

    // 이리온 정보 페이지
    @GetMapping("/info")
    public String info() {
        return "info/profile";
    }

}
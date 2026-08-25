package com.irion.common.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 확장자 없는 정규 주소로 페이지를 내보낸다. 페이지는 WEB-INF 안이라 직접 열 수 없다.
 * 옛 .html 주소는 LegacyHtmlRedirectFilter 가 301 한다.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/info")
    public String info() {
        return "info";
    }

    @GetMapping("/schedule")
    public String schedule() {
        return "schedule";
    }

}

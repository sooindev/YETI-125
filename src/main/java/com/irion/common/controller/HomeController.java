package com.irion.common.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 확장자 없는 정규 주소로 페이지를 내보낸다.
 *
 * 뷰 이름만 돌려주면 InternalResourceViewResolver 가 /WEB-INF/views/<이름>.jsp
 * 를 찾아 그린다. 주소창은 /schedule 그대로 남는다.
 *
 * 페이지가 WEB-INF 안에 있어 바깥에서 직접 열 수 없다 — 주소는 여기 적힌
 * 것 하나뿐이다. 예전 .html 주소는 LegacyHtmlRedirectFilter 가 301 한다.
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

package com.irion.common.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 확장자 없는 정규 주소로 페이지를 내보낸다.
 *
 * redirect 가 아니라 forward 다. redirect 면 주소창이 다시 .html 로 바뀌어
 * 정규 주소가 뒤집힌다. forward 는 주소를 그대로 둔 채 내용만 꺼내온다.
 * 실제 파일은 DispatcherServlet 뒤의 default-servlet-handler 가 읽는다.
 *
 * 예전 .html 주소는 LegacyHtmlRedirectFilter 가 여기로 301 한다.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "forward:/index.html";
    }

    @GetMapping("/info")
    public String info() {
        return "forward:/info.html";
    }

    @GetMapping("/schedule")
    public String schedule() {
        return "forward:/schedule.html";
    }

}

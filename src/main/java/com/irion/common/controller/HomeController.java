package com.irion.common.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "redirect:/index.html";
    }

    @GetMapping("/info")
    public String info() {
        return "redirect:/info.html";
    }

    @GetMapping("/schedule")
    public String schedule() {
        return "redirect:/schedule.html";
    }

}
package com.irion.schedule.controller;

import com.irion.common.util.JsonResult;
import com.irion.schedule.service.ScheduleService;
import com.irion.schedule.vo.ScheduleVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("/schedule")
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    /** 일정 목록 조회 (FullCalendar용) */
    @GetMapping("/list")
    @ResponseBody
    public List<Map<String, Object>> getScheduleList(@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date start, @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date end) {

        List<ScheduleVO> scheduleList = scheduleService.getDisplayScheduleList(start, end);
        List<Map<String, Object>> events = new ArrayList<>();

        for (ScheduleVO s : scheduleList) {
            Map<String, Object> event = new HashMap<>();
            event.put("id", s.getScheduleId());
            event.put("title", s.getTitle());
            event.put("start", s.getStartDate());
            event.put("end", s.getEndDate());
            event.put("allDay", "Y".equals(s.getAllDayYn()));
            event.put("color", s.getColor());
            event.put("description", s.getDescription());
            event.put("type", s.getScheduleType());
            events.add(event);
        }

        return events;
    }

    /** 일정 상세 조회 */
    @GetMapping("/{scheduleId}")
    @ResponseBody
    public JsonResult getSchedule(@PathVariable Long scheduleId) {
        ScheduleVO schedule = scheduleService.getSchedule(scheduleId);

        // 일정 없거나 비공개
        if (schedule == null || !"Y".equals(schedule.getDisplayYn())) {
            return JsonResult.fail("일정을 찾을 수 없습니다.");
        }

        return JsonResult.success("조회 성공", schedule);
    }
}
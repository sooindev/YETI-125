package com.irion.schedule.controller;

import com.irion.common.util.JsonResult;
import com.irion.schedule.service.ScheduleService;
import com.irion.schedule.vo.ScheduleVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("/schedule")
public class ScheduleController {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleController.class);

    @Autowired
    private ScheduleService scheduleService;

    // 일정 목록 조회 (JSON) - FullCalendar용
    @GetMapping("/list")
    @ResponseBody
    public List<Map<String, Object>> getScheduleList(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date start,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date end) {

        logger.debug("Getting schedule list: {} ~ {}", start, end);

        List<ScheduleVO> scheduleList = scheduleService.getDisplayScheduleList(start, end);
        List<Map<String, Object>> events = new ArrayList<>();

        for (ScheduleVO schedule : scheduleList) {
            Map<String, Object> event = new HashMap<>();
            event.put("id", schedule.getScheduleId());
            event.put("title", schedule.getTitle());
            event.put("start", schedule.getStartDate());
            event.put("end", schedule.getEndDate());
            event.put("allDay", "Y".equals(schedule.getAllDayYn()));
            event.put("color", schedule.getColor());
            event.put("description", schedule.getDescription());
            event.put("type", schedule.getScheduleType());
            events.add(event);
        }

        return events;
    }

    // 일정 상세 조회 (JSON)
    @GetMapping("/{scheduleId}")
    @ResponseBody
    public JsonResult getSchedule(@PathVariable Long scheduleId) {
        ScheduleVO schedule = scheduleService.getSchedule(scheduleId);
        if (schedule == null || !"Y".equals(schedule.getDisplayYn())) {
            return JsonResult.fail("일정을 찾을 수 없습니다.");
        }
        return JsonResult.success("조회 성공", schedule);
    }

}
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
@RequestMapping("/admin/schedule")
public class AdminScheduleController {

    private static final Logger logger = LoggerFactory.getLogger(AdminScheduleController.class);

    @Autowired
    private ScheduleService scheduleService;

    // 일정 관리 페이지
    @GetMapping("")
    public String scheduleManage() {
        return "redirect:/admin/admin-schedule.html";
    }

    // 일정 목록 조회 (JSON)
    @GetMapping("/list")
    @ResponseBody
    public List<Map<String, Object>> getScheduleList(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date start,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date end) {

        logger.debug("Admin getting schedule list: {} ~ {}", start, end);

        List<ScheduleVO> scheduleList = scheduleService.getScheduleList(start, end);
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
            event.put("displayYn", schedule.getDisplayYn());
            events.add(event);
        }

        return events;
    }

    // 일정 상세 조회
    @GetMapping("/{scheduleId}")
    @ResponseBody
    public JsonResult getSchedule(@PathVariable Long scheduleId) {
        ScheduleVO schedule = scheduleService.getSchedule(scheduleId);
        if (schedule == null) {
            return JsonResult.fail("일정을 찾을 수 없습니다.");
        }
        return JsonResult.success("조회 성공", schedule);
    }

    // 일정 등록
    @PostMapping("")
    @ResponseBody
    public JsonResult createSchedule(@RequestBody ScheduleVO scheduleVO) {
        logger.info("Creating schedule: {}", scheduleVO.getTitle());

        try {
            Long scheduleId = scheduleService.createSchedule(scheduleVO);
            if (scheduleId != null) {
                return JsonResult.success("일정이 등록되었습니다.", scheduleId);
            }
            return JsonResult.fail("일정 등록에 실패했습니다.");
        } catch (Exception e) {
            logger.error("Error creating schedule", e);
            return JsonResult.fail("오류가 발생했습니다.");
        }
    }

    // 일정 수정
    @PutMapping("/{scheduleId}")
    @ResponseBody
    public JsonResult updateSchedule(@PathVariable Long scheduleId,
                                     @RequestBody ScheduleVO scheduleVO) {

        logger.info("Updating schedule: {}", scheduleId);
        scheduleVO.setScheduleId(scheduleId);

        try {
            boolean success = scheduleService.updateSchedule(scheduleVO);
            if (success) {
                return JsonResult.success("일정이 수정되었습니다.");
            }
            return JsonResult.fail("일정 수정에 실패했습니다.");
        } catch (Exception e) {
            logger.error("Error updating schedule", e);
            return JsonResult.fail("오류가 발생했습니다.");
        }
    }

    // 일정 삭제
    @DeleteMapping("/{scheduleId}")
    @ResponseBody
    public JsonResult deleteSchedule(@PathVariable Long scheduleId) {
        logger.info("Deleting schedule: {}", scheduleId);

        try {
            boolean success = scheduleService.deleteSchedule(scheduleId);
            if (success) {
                return JsonResult.success("일정이 삭제되었습니다.");
            }
            return JsonResult.fail("일정 삭제에 실패했습니다.");
        } catch (Exception e) {
            logger.error("Error deleting schedule", e);
            return JsonResult.fail("오류가 발생했습니다.");
        }
    }

}
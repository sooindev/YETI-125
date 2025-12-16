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
@RequestMapping("/admin/schedule")
public class AdminScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    /** 일정 관리 페이지 */
    @GetMapping("")
    public String scheduleManage() {
        return "redirect:/admin/admin-schedule.html";
    }

    /** 일정 목록 조회 */
    @GetMapping("/list")
    @ResponseBody
    public List<Map<String, Object>> getScheduleList(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date start,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date end) {

        List<ScheduleVO> scheduleList = scheduleService.getScheduleList(start, end);
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
            event.put("displayYn", s.getDisplayYn());
            events.add(event);
        }

        return events;
    }

    /** 일정 상세 조회 */
    @GetMapping("/{scheduleId}")
    @ResponseBody
    public JsonResult getSchedule(@PathVariable Long scheduleId) {
        ScheduleVO schedule = scheduleService.getSchedule(scheduleId);

        if (schedule == null) {
            return JsonResult.fail("일정을 찾을 수 없습니다.");
        }

        return JsonResult.success("조회 성공", schedule);
    }

    /** 일정 등록 */
    @PostMapping("")
    @ResponseBody
    public JsonResult createSchedule(@RequestBody ScheduleVO scheduleVO) {
        Long scheduleId = scheduleService.createSchedule(scheduleVO);

        if (scheduleId == null) {
            return JsonResult.fail("일정 등록에 실패했습니다.");
        }

        return JsonResult.success("일정이 등록되었습니다.", scheduleId);
    }

    /** 일정 수정 */
    @PutMapping("/{scheduleId}")
    @ResponseBody
    public JsonResult updateSchedule(@PathVariable Long scheduleId, @RequestBody ScheduleVO scheduleVO) {
        scheduleVO.setScheduleId(scheduleId);
        boolean success = scheduleService.updateSchedule(scheduleVO);

        if (!success) {
            return JsonResult.fail("일정 수정에 실패했습니다.");
        }

        return JsonResult.success("일정이 수정되었습니다.");
    }

    /** 일정 삭제 */
    @DeleteMapping("/{scheduleId}")
    @ResponseBody
    public JsonResult deleteSchedule(@PathVariable Long scheduleId) {
        boolean success = scheduleService.deleteSchedule(scheduleId);

        if (!success) {
            return JsonResult.fail("일정 삭제에 실패했습니다.");
        }

        return JsonResult.success("일정이 삭제되었습니다.");
    }
}
package com.irion.schedule.controller;

import com.irion.common.util.DateRange;
import com.irion.common.util.JsonResult;
import com.irion.schedule.service.ScheduleService;
import com.irion.schedule.vo.ScheduleVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
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

        // 요청한 기간이 아무리 넓어도 상한까지만 본다
        List<ScheduleVO> scheduleList =
                scheduleService.getScheduleList(start, DateRange.clampEnd(start, end));
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
    public JsonResult createSchedule(@Valid @RequestBody ScheduleVO scheduleVO, BindingResult binding) {
        if (binding.hasErrors()) {
            return JsonResult.fail(firstMessage(binding));
        }

        Long scheduleId = scheduleService.createSchedule(scheduleVO);

        if (scheduleId == null) {
            return JsonResult.fail("일정 등록에 실패했습니다.");
        }

        return JsonResult.success("일정이 등록되었습니다.", scheduleId);
    }

    /** 일정 수정 */
    @PutMapping("/{scheduleId}")
    @ResponseBody
    public JsonResult updateSchedule(@PathVariable Long scheduleId,
                                     @Valid @RequestBody ScheduleVO scheduleVO,
                                     BindingResult binding) {
        if (binding.hasErrors()) {
            return JsonResult.fail(firstMessage(binding));
        }

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

    /**
     * 첫 번째 검증 오류 메시지.
     *
     * 화면은 토스트 한 줄로 보여주므로 하나만 골라 준다. 여기서 막지 않으면
     * 길이 초과가 DB 제약에 걸려 500 이 나가고, 사용자는 무엇이 잘못됐는지
     * 알 수 없다.
     */
    private String firstMessage(BindingResult binding) {
        FieldError error = binding.getFieldError();
        if (error != null && error.getDefaultMessage() != null) {
            return error.getDefaultMessage();
        }
        return "입력값이 올바르지 않습니다.";
    }
}
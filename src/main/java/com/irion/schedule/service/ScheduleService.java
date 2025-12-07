package com.irion.schedule.service;

import com.irion.schedule.vo.ScheduleVO;

import java.util.Date;
import java.util.List;

public interface ScheduleService {

    // 일정 목록 조회 (기간별, 관리자용)
    List<ScheduleVO> getScheduleList(Date startDate, Date endDate);

    // 공개된 일정 목록 조회 (사용자용)
    List<ScheduleVO> getDisplayScheduleList(Date startDate, Date endDate);

    // 일정 상세 조회
    ScheduleVO getSchedule(Long scheduleId);

    // 일정 등록
    Long createSchedule(ScheduleVO scheduleVO);

    // 일정 수정
    boolean updateSchedule(ScheduleVO scheduleVO);

    // 일정 삭제
    boolean deleteSchedule(Long scheduleId);

}
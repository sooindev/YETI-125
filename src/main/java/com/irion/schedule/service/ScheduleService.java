package com.irion.schedule.service;

import com.irion.schedule.vo.ScheduleVO;

import java.util.Date;
import java.util.List;

public interface ScheduleService {

    List<ScheduleVO> getScheduleList(Date startDate, Date endDate);

    List<ScheduleVO> getDisplayScheduleList(Date startDate, Date endDate);

    ScheduleVO getSchedule(Long scheduleId);

    Long createSchedule(ScheduleVO scheduleVO);

    boolean updateSchedule(ScheduleVO scheduleVO);

    boolean deleteSchedule(Long scheduleId);

}
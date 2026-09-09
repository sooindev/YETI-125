package com.irion.feature.schedule.service;

import com.irion.feature.schedule.domain.ScheduleVO;

import java.util.Date;
import java.util.List;

public interface ScheduleService {

    List<ScheduleVO> getScheduleList(Date startDate, Date endDate);

    List<ScheduleVO> getDisplayScheduleList(Date startDate, Date endDate);

    Long createSchedule(ScheduleVO scheduleVO);

    boolean updateSchedule(ScheduleVO scheduleVO);

    boolean deleteSchedule(Long scheduleId);

}
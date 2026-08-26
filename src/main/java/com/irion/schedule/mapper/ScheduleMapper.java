package com.irion.schedule.mapper;

import com.irion.schedule.vo.ScheduleVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface ScheduleMapper {

    List<ScheduleVO> selectScheduleList(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

    List<ScheduleVO> selectDisplayScheduleList(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

    int insertSchedule(ScheduleVO scheduleVO);

    int updateSchedule(ScheduleVO scheduleVO);

    int deleteSchedule(@Param("scheduleId") Long scheduleId);

}
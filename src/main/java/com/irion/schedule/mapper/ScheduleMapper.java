package com.irion.schedule.mapper;

import com.irion.schedule.vo.ScheduleVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface ScheduleMapper {

    // 일정 목록 조회 (기간별, 관리자용 - 모든 일정)
    List<ScheduleVO> selectScheduleList(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

    // 공개된 일정 목록 조회 (사용자용)
    List<ScheduleVO> selectDisplayScheduleList(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

    // 일정 상세 조회
    ScheduleVO selectScheduleById(@Param("scheduleId") Long scheduleId);

    // 일정 등록
    int insertSchedule(ScheduleVO scheduleVO);

    // 일정 수정
    int updateSchedule(ScheduleVO scheduleVO);

    // 일정 삭제 (소프트 삭제)
    int deleteSchedule(@Param("scheduleId") Long scheduleId);

}
package com.irion.schedule.service.impl;

import com.irion.schedule.mapper.ScheduleMapper;
import com.irion.schedule.service.ScheduleService;
import com.irion.schedule.vo.ScheduleVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class ScheduleServiceImpl implements ScheduleService {

    @Autowired
    private ScheduleMapper scheduleMapper;

    /** 전체 일정 목록 (관리자용) */
    @Override
    public List<ScheduleVO> getScheduleList(Date startDate, Date endDate) {
        return scheduleMapper.selectScheduleList(startDate, endDate);
    }

    /** 공개 일정 목록 (사용자용) */
    @Override
    public List<ScheduleVO> getDisplayScheduleList(Date startDate, Date endDate) {
        return scheduleMapper.selectDisplayScheduleList(startDate, endDate);
    }

    /** 일정 상세 조회 */
    @Override
    public ScheduleVO getSchedule(Long scheduleId) {
        return scheduleMapper.selectScheduleById(scheduleId);
    }

    /** 일정 등록 */
    @Override
    @Transactional
    public Long createSchedule(ScheduleVO schedule) {
        setDefaults(schedule);

        // 등록할 때만 기본값을 준다 — 새 일정에는 이미 정해진 값이 없다
        if (schedule.getDisplayYn() == null || schedule.getDisplayYn().isEmpty())
            schedule.setDisplayYn("Y");

        int result = scheduleMapper.insertSchedule(schedule);
        return result > 0 ? schedule.getScheduleId() : null;
    }

    /** 일정 수정 */
    @Override
    @Transactional
    public boolean updateSchedule(ScheduleVO schedule) {
        // display_yn 은 빼고 채운다 — 빠져 있으면 SQL 이 건드리지 않아 기존 값이 남는다
        setDefaults(schedule);

        return scheduleMapper.updateSchedule(schedule) > 0;
    }

    /** 일정 삭제 */
    @Override
    @Transactional
    public boolean deleteSchedule(Long scheduleId) {
        return scheduleMapper.deleteSchedule(scheduleId) > 0;
    }

    /**
     * schema.sql 의 DEFAULT 와 짝을 맞춘 값. INSERT/UPDATE 가 컬럼을 전부 나열해서
     * 컬럼 DEFAULT 가 안 먹는다. displayYn 은 여기서 다루지 않는다.
     */
    private void setDefaults(ScheduleVO schedule) {
        if (schedule.getScheduleType() == null || schedule.getScheduleType().isEmpty())
            schedule.setScheduleType("STREAM");
        if (schedule.getAllDayYn() == null || schedule.getAllDayYn().isEmpty())
            schedule.setAllDayYn("N");
        if (schedule.getColor() == null || schedule.getColor().isEmpty())
            schedule.setColor("#6366F1");
    }
}
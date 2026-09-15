package com.irion.feature.schedule.service.impl;

import com.irion.feature.schedule.persistence.ScheduleMapper;
import com.irion.feature.schedule.service.ScheduleService;
import com.irion.feature.schedule.domain.ScheduleVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class ScheduleServiceImpl implements ScheduleService {

    @Autowired
    private ScheduleMapper scheduleMapper;

    /** 전체 일정 — 관리자용 */
    @Override
    public List<ScheduleVO> getScheduleList(Date startDate, Date endDate) {
        return scheduleMapper.selectScheduleList(startDate, endDate);
    }

    /** 공개 일정 — 사용자용 */
    @Override
    public List<ScheduleVO> getDisplayScheduleList(Date startDate, Date endDate) {
        return scheduleMapper.selectDisplayScheduleList(startDate, endDate);
    }

    /** 등록 */
    @Override
    @Transactional
    public Long createSchedule(ScheduleVO schedule) {
        setDefaults(schedule);

        // 등록에만 기본값 — 새 일정에는 정해진 값이 없다
        if (schedule.getDisplayYn() == null || schedule.getDisplayYn().isEmpty())
            schedule.setDisplayYn("Y");

        int result = scheduleMapper.insertSchedule(schedule);
        return result > 0 ? schedule.getScheduleId() : null;
    }

    /** 수정 */
    @Override
    @Transactional
    public boolean updateSchedule(ScheduleVO schedule) {
        // display_yn 제외 — 빠지면 SQL 이 건드리지 않아 기존 값 유지
        setDefaults(schedule);

        return scheduleMapper.updateSchedule(schedule) > 0;
    }

    /** 삭제 */
    @Override
    @Transactional
    public boolean deleteSchedule(Long scheduleId) {
        return scheduleMapper.deleteSchedule(scheduleId) > 0;
    }

    /**
     * schema.sql 의 DEFAULT 와 짝을 맞춘 값.
     * INSERT/UPDATE 가 컬럼을 전부 나열해 컬럼 DEFAULT 가 안 먹는다(displayYn 제외)
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
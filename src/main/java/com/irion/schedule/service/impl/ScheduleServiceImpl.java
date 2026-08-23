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

        // 공개 여부는 등록할 때만 기본값을 준다. 새 일정에는 "이미 정해진 값" 이
        // 없기 때문이다. 수정은 다르다 — updateSchedule 참고
        if (schedule.getDisplayYn() == null || schedule.getDisplayYn().isEmpty())
            schedule.setDisplayYn("Y");

        int result = scheduleMapper.insertSchedule(schedule);
        return result > 0 ? schedule.getScheduleId() : null;
    }

    /** 일정 수정 */
    @Override
    @Transactional
    public boolean updateSchedule(ScheduleVO schedule) {
        // NOT NULL 컬럼을 조건 없이 덮어쓰므로 기본값을 거쳐야 한다.
        // display_yn 만 예외 — 빠져 있으면 SQL 이 그 컬럼을 건드리지 않아
        // DB 의 기존 값이 남는다
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
     * schema.sql 의 DEFAULT 와 짝을 맞춘 값.
     *
     * 컬럼에 DEFAULT 가 있어도 INSERT/UPDATE 가 그 컬럼을 직접 지정하면 쓰이지
     * 않는다. 두 문 모두 컬럼을 전부 나열하므로 NOT NULL 이 null 로 오면 DB 오류다.
     * 검증 규칙이 빈 문자열을 허용하므로 null 만 막아서는 부족하다.
     *
     * displayYn 은 여기서 다루지 않는다 (createSchedule / updateSchedule 참고).
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
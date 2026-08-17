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
        // 기본값 설정
        setDefaults(schedule);

        int result = scheduleMapper.insertSchedule(schedule);
        return result > 0 ? schedule.getScheduleId() : null;
    }

    /** 일정 수정 */
    @Override
    @Transactional
    public boolean updateSchedule(ScheduleVO schedule) {
        return scheduleMapper.updateSchedule(schedule) > 0;
    }

    /** 일정 삭제 */
    @Override
    @Transactional
    public boolean deleteSchedule(Long scheduleId) {
        return scheduleMapper.deleteSchedule(scheduleId) > 0;
    }

    /** 기본값 설정 */
    private void setDefaults(ScheduleVO schedule) {
        if (schedule.getAllDayYn() == null)
            schedule.setAllDayYn("N");
        if (schedule.getDisplayYn() == null)
            schedule.setDisplayYn("Y");
        if (schedule.getColor() == null)
            schedule.setColor("#6366F1");
    }
}
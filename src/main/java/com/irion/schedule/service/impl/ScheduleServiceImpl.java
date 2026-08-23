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

        /*
         * 공개 여부는 등록할 때만 기본값을 준다.
         *
         * 새로 만드는 일정에는 "아직 정해진 값" 이라는 게 없으므로,
         * schema.sql 의 DEFAULT 와 같은 'Y' 로 시작한다. 수정은 다르다
         * — 이미 정해진 값이 있고, 그것을 함부로 바꾸면 안 된다.
         * (updateSchedule 과 Schedule_SQL.xml 의 주석 참고)
         */
        if (schedule.getDisplayYn() == null || schedule.getDisplayYn().isEmpty())
            schedule.setDisplayYn("Y");

        int result = scheduleMapper.insertSchedule(schedule);
        return result > 0 ? schedule.getScheduleId() : null;
    }

    /** 일정 수정 */
    @Override
    @Transactional
    public boolean updateSchedule(ScheduleVO schedule) {
        /*
         * 수정도 등록과 같은 기본값을 거쳐야 한다.
         *
         * updateSchedule 의 SQL 은 NOT NULL 컬럼(schedule_type, all_day_yn)을
         * 조건 없이 덮어쓴다. 값이 빠진 채로 들어오면 DB 가 거부해서 500 이
         * 나간다.
         *
         * display_yn 은 여기서 채우지 않는다. 빠져 있으면 SQL 이 그 컬럼을
         * 아예 건드리지 않아 DB 의 기존 값이 남는다.
         */
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
     * 기본값 설정.
     *
     * 여기 있는 값은 schema.sql 의 DEFAULT 와 짝을 맞춘 것이다. 컬럼에
     * DEFAULT 가 걸려 있어도 INSERT/UPDATE 문이 그 컬럼을 직접 지정하면
     * DEFAULT 는 쓰이지 않는다. 두 문 모두 여덟 컬럼을 전부 나열하므로,
     * NOT NULL 컬럼이 null 로 오면 그대로 DB 오류가 된다.
     *
     * displayYn 은 여기서 다루지 않는다 — 등록에서만 값을 정하고, 수정은
     * 빠져 있으면 DB 의 기존 값을 그대로 둔다.
     *
     * scheduleType 은 여기 빠져 있었다. 관리자 화면이 늘 값을 실어 보내서
     * 드러나지 않았을 뿐, 이 값을 안 보내는 호출이 하나라도 생기면
     * "저장 중 오류가 발생했습니다" 만 뜨고 원인은 로그를 봐야 알 수 있었다.
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
package com.irion.schedule.service.impl;

import com.irion.schedule.mapper.ScheduleMapper;
import com.irion.schedule.service.ScheduleService;
import com.irion.schedule.vo.ScheduleVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class ScheduleServiceImpl implements ScheduleService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleServiceImpl.class);

    @Autowired
    private ScheduleMapper scheduleMapper;

    @Override
    public List<ScheduleVO> getScheduleList(Date startDate, Date endDate) {
        logger.debug("Getting schedule list: {} ~ {}", startDate, endDate);
        return scheduleMapper.selectScheduleList(startDate, endDate);
    }

    @Override
    public List<ScheduleVO> getDisplayScheduleList(Date startDate, Date endDate) {
        logger.debug("Getting display schedule list: {} ~ {}", startDate, endDate);
        return scheduleMapper.selectDisplayScheduleList(startDate, endDate);
    }

    @Override
    public ScheduleVO getSchedule(Long scheduleId) {
        logger.debug("Getting schedule: {}", scheduleId);
        return scheduleMapper.selectScheduleById(scheduleId);
    }

    @Override
    @Transactional
    public Long createSchedule(ScheduleVO scheduleVO) {
        logger.info("Creating schedule: {}", scheduleVO.getTitle());

        // 기본값 설정
        if (scheduleVO.getAllDayYn() == null) {
            scheduleVO.setAllDayYn("N");
        }
        if (scheduleVO.getDisplayYn() == null) {
            scheduleVO.setDisplayYn("Y");
        }
        if (scheduleVO.getColor() == null) {
            scheduleVO.setColor("#6366F1");
        }

        int result = scheduleMapper.insertSchedule(scheduleVO);
        if (result > 0) {
            logger.info("Schedule created: {}", scheduleVO.getScheduleId());
            return scheduleVO.getScheduleId();
        }
        return null;
    }

    @Override
    @Transactional
    public boolean updateSchedule(ScheduleVO scheduleVO) {
        logger.info("Updating schedule: {}", scheduleVO.getScheduleId());
        int result = scheduleMapper.updateSchedule(scheduleVO);
        return result > 0;
    }

    @Override
    @Transactional
    public boolean deleteSchedule(Long scheduleId) {
        logger.info("Deleting schedule: {}", scheduleId);
        int result = scheduleMapper.deleteSchedule(scheduleId);
        return result > 0;
    }

}
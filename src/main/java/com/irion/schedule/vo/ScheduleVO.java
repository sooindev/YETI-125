package com.irion.schedule.vo;

import java.io.Serializable;
import java.util.Date;

public class ScheduleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long scheduleId;
    private String title;
    private String description;
    private String scheduleType;      // STREAM: 방송, EVENT: 이벤트, OTHER: 기타
    private Date startDate;
    private Date endDate;
    private String allDayYn;          // Y: 종일, N: 시간 지정
    private String displayYn;         // Y: 표시, N: 숨김
    private String color;             // 캘린더 색상 (HEX)
    private Date regDate;
    private Date modDate;
    private String delYn;

    public ScheduleVO() {
    }

    public Long getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(Long scheduleId) {
        this.scheduleId = scheduleId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getScheduleType() {
        return scheduleType;
    }

    public void setScheduleType(String scheduleType) {
        this.scheduleType = scheduleType;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getAllDayYn() {
        return allDayYn;
    }

    public void setAllDayYn(String allDayYn) {
        this.allDayYn = allDayYn;
    }

    public String getDisplayYn() {
        return displayYn;
    }

    public void setDisplayYn(String displayYn) {
        this.displayYn = displayYn;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Date getRegDate() {
        return regDate;
    }

    public void setRegDate(Date regDate) {
        this.regDate = regDate;
    }

    public Date getModDate() {
        return modDate;
    }

    public void setModDate(Date modDate) {
        this.modDate = modDate;
    }

    public String getDelYn() {
        return delYn;
    }

    public void setDelYn(String delYn) {
        this.delYn = delYn;
    }

    @Override
    public String toString() {
        return "ScheduleVO{" +
                "scheduleId=" + scheduleId +
                ", title='" + title + '\'' +
                ", scheduleType='" + scheduleType + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                '}';
    }
}
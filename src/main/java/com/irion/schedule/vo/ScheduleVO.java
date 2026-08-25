package com.irion.schedule.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;

public class ScheduleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    // 제약은 tb_schedule 컬럼 정의에서 따왔다. 여기서 안 걸러내면 DB 제약에 걸려 500 이 난다.

    private Long scheduleId;

    @NotBlank(message = "제목을 입력해 주세요.")
    @Size(max = 200, message = "제목은 200자를 넘을 수 없습니다.")
    private String title;

    @Size(max = 5000, message = "설명은 5000자를 넘을 수 없습니다.")
    private String description;

    @Pattern(regexp = "(STREAM|COLLAB|JUSTCHAT|GAME|KARAOKE|EVENT|OTHER)?",
             message = "알 수 없는 일정 유형입니다.")
    private String scheduleType;

    @NotNull(message = "시작 일시를 선택해 주세요.")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
    private Date startDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
    private Date endDate;

    @Pattern(regexp = "[YN]?", message = "종일 여부가 올바르지 않습니다.")
    private String allDayYn;

    @Pattern(regexp = "[YN]?", message = "공개 여부가 올바르지 않습니다.")
    private String displayYn;

    @Pattern(regexp = "(#[0-9a-fA-F]{3}([0-9a-fA-F]{3})?)?", message = "색상 형식이 올바르지 않습니다.")
    @Size(max = 10, message = "색상 값이 너무 깁니다.")
    private String color;
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
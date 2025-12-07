package com.irion.admin.vo;

import java.io.Serializable;
import java.util.Date;

public class AdminVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long adminId;
    private String adminLoginId;
    private String adminPassword;
    private String adminName;
    private Date lastLoginDate;
    private Date regDate;
    private Date modDate;
    private String delYn;

    public AdminVO() {
    }

    public Long getAdminId() {
        return adminId;
    }

    public void setAdminId(Long adminId) {
        this.adminId = adminId;
    }

    public String getAdminLoginId() {
        return adminLoginId;
    }

    public void setAdminLoginId(String adminLoginId) {
        this.adminLoginId = adminLoginId;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public String getAdminName() {
        return adminName;
    }

    public void setAdminName(String adminName) {
        this.adminName = adminName;
    }

    public Date getLastLoginDate() {
        return lastLoginDate;
    }

    public void setLastLoginDate(Date lastLoginDate) {
        this.lastLoginDate = lastLoginDate;
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
        return "AdminVO{" +
                "adminId=" + adminId +
                ", adminLoginId='" + adminLoginId + '\'' +
                ", adminName='" + adminName + '\'' +
                '}';
    }
}
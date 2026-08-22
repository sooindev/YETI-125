package com.irion.admin.service;

import com.irion.admin.vo.AdminVO;

public interface AdminService {

    // 관리자 로그인
    AdminVO login(String adminLoginId, String password);

}
package com.irion.admin.service.impl;

import com.irion.admin.mapper.AdminMapper;
import com.irion.admin.service.AdminService;
import com.irion.admin.vo.AdminVO;
import com.irion.common.util.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private AdminMapper adminMapper;

    @Override
    @Transactional
    public AdminVO login(String adminLoginId, String password) {
        // 관리자 조회
        AdminVO admin = adminMapper.selectAdminByLoginId(adminLoginId);

        // 검증 실패
        if (admin == null || !PasswordUtil.matches(password, admin.getAdminPassword())) {
            return null;
        }

        // 로그인 성공 - 마지막 로그인 시간 업데이트
        adminMapper.updateLastLoginDate(admin.getAdminId());
        return admin;
    }

    @Override
    public AdminVO getAdminInfo(Long adminId) {
        return adminMapper.selectAdminById(adminId);
    }
}
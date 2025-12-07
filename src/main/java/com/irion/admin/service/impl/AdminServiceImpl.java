package com.irion.admin.service.impl;

import com.irion.admin.mapper.AdminMapper;
import com.irion.admin.service.AdminService;
import com.irion.admin.vo.AdminVO;
import com.irion.common.util.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminServiceImpl implements AdminService {

    private static final Logger logger = LoggerFactory.getLogger(AdminServiceImpl.class);

    @Autowired
    private AdminMapper adminMapper;

    @Override
    @Transactional
    public AdminVO login(String adminLoginId, String password) {
        logger.debug("Login attempt: {}", adminLoginId);

        // 관리자 정보 조회
        AdminVO admin = adminMapper.selectAdminByLoginId(adminLoginId);

        if (admin == null) {
            logger.debug("Admin not found: {}", adminLoginId);
            return null;
        }

        // 비밀번호 검증
        if (!PasswordUtil.matches(password, admin.getAdminPassword())) {
            logger.debug("Password mismatch for: {}", adminLoginId);
            return null;
        }

        // 마지막 로그인 시간 업데이트
        adminMapper.updateLastLoginDate(admin.getAdminId());

        logger.info("Login success: {}", adminLoginId);
        return admin;
    }

    @Override
    public AdminVO getAdminInfo(Long adminId) {
        return adminMapper.selectAdminById(adminId);
    }

}
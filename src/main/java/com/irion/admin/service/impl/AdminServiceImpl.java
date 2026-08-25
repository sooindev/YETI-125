package com.irion.admin.service.impl;

import com.irion.admin.mapper.AdminMapper;
import com.irion.admin.service.AdminService;
import com.irion.admin.vo.AdminVO;
import com.irion.common.util.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminServiceImpl implements AdminService {

    private static final Logger logger = LoggerFactory.getLogger(AdminServiceImpl.class);

    @Autowired
    private AdminMapper adminMapper;

    @Override
    @Transactional
    public AdminVO login(String adminLoginId, String password) {
        AdminVO admin = adminMapper.selectAdminByLoginId(adminLoginId);

        // 없는 아이디에도 같은 시간을 쓴다 — 안 그러면 응답 시간으로 계정 존재가 드러난다
        if (admin == null) {
            PasswordUtil.matchesDummy(password);
            return null;
        }

        if (!PasswordUtil.matches(password, admin.getAdminPassword())) {
            return null;
        }

        adminMapper.updateLastLoginDate(admin.getAdminId());

        // 원문을 아는 시점은 로그인 성공뿐이다. 재해시에 실패해도 로그인은 성공시킨다
        if (PasswordUtil.needsUpgrade(admin.getAdminPassword())) {
            try {
                adminMapper.updatePassword(admin.getAdminId(), PasswordUtil.encode(password));
                logger.info("Admin password hash upgraded: adminId={}", admin.getAdminId());
            } catch (Exception e) {
                logger.warn("Admin password hash upgrade failed: adminId={}", admin.getAdminId(), e);
            }
        }

        return admin;
    }
}
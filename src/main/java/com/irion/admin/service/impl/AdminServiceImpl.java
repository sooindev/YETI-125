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

        // 없는 아이디여도 검증에 드는 시간을 그대로 쓴다 — 곧장 돌아서면
        // 응답 시간만으로 계정 존재 여부가 드러난다
        if (admin == null) {
            PasswordUtil.matchesDummy(password);
            return null;
        }

        if (!PasswordUtil.matches(password, admin.getAdminPassword())) {
            return null;
        }

        adminMapper.updateLastLoginDate(admin.getAdminId());

        // 반복 횟수를 올렸으면 여기서 다시 해시한다. 원문을 아는 시점은
        // 로그인 성공뿐이다. 실패해도 로그인은 그대로 성공시킨다
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
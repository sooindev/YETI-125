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
        // 관리자 조회
        AdminVO admin = adminMapper.selectAdminByLoginId(adminLoginId);

        // 검증 실패
        if (admin == null || !PasswordUtil.matches(password, admin.getAdminPassword())) {
            return null;
        }

        // 로그인 성공 - 마지막 로그인 시간 업데이트
        adminMapper.updateLastLoginDate(admin.getAdminId());

        /*
         * 옛 형식으로 저장된 해시를 이 자리에서 새 형식으로 바꾼다.
         * 원문을 아는 시점은 여기뿐이라, DB 를 한 번에 갈아엎지 않고
         * 로그인하는 계정부터 조용히 옮겨간다.
         *
         * 실패해도 로그인은 그대로 성공시킨다. 다음 로그인에 다시 시도된다.
         */
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
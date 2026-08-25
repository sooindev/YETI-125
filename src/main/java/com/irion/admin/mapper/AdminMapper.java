package com.irion.admin.mapper;

import com.irion.admin.vo.AdminVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminMapper {

    AdminVO selectAdminByLoginId(@Param("adminLoginId") String adminLoginId);

    int updateLastLoginDate(@Param("adminId") Long adminId);

    // 비밀번호 해시 갱신 (옛 형식 → PBKDF2 재해시)
    int updatePassword(@Param("adminId") Long adminId,
                       @Param("adminPassword") String adminPassword);

}
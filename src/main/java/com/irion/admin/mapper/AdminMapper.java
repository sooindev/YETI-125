package com.irion.admin.mapper;

import com.irion.admin.vo.AdminVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminMapper {

    // 로그인 ID로 관리자 조회
    AdminVO selectAdminByLoginId(@Param("adminLoginId") String adminLoginId);

    // 관리자 정보 조회
    AdminVO selectAdminById(@Param("adminId") Long adminId);

    // 마지막 로그인 시간 업데이트
    int updateLastLoginDate(@Param("adminId") Long adminId);

}
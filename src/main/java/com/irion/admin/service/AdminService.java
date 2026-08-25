package com.irion.admin.service;

import com.irion.admin.vo.AdminVO;

public interface AdminService {

    AdminVO login(String adminLoginId, String password);

}
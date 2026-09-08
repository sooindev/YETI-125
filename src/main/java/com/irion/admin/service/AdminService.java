package com.irion.admin.service;

import com.irion.admin.domain.AdminVO;

public interface AdminService {

    AdminVO login(String adminLoginId, String password);

}
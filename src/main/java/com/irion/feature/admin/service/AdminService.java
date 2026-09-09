package com.irion.feature.admin.service;

import com.irion.feature.admin.domain.AdminVO;

public interface AdminService {

    AdminVO login(String adminLoginId, String password);

}
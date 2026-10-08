package com.internconnect.service;

import com.internconnect.dto.AuthResponse;
import com.internconnect.dto.CompanyRegisterRequest;
import com.internconnect.dto.LoginRequest;
import com.internconnect.dto.StudentRegisterRequest;

public interface AuthService {
    AuthResponse registerStudent(StudentRegisterRequest request);
    AuthResponse registerCompany(CompanyRegisterRequest request);
    AuthResponse login(LoginRequest request);
}

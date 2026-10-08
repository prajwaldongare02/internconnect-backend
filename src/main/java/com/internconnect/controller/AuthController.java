package com.internconnect.controller;

import com.internconnect.dto.*;
import com.internconnect.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register/student")
    public ResponseEntity<ApiResponse<AuthResponse>> registerStudent(
            @Valid @RequestBody StudentRegisterRequest request) {
        AuthResponse response = authService.registerStudent(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Student registered successfully", response));
    }

    @PostMapping("/register/company")
    public ResponseEntity<ApiResponse<AuthResponse>> registerCompany(
            @Valid @RequestBody CompanyRegisterRequest request) {
        AuthResponse response = authService.registerCompany(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Company registered successfully. Pending admin approval.", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }
}

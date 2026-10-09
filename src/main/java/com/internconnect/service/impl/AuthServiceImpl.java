package com.internconnect.service.impl;

import com.internconnect.dto.*;
import com.internconnect.entity.*;
import com.internconnect.exception.BadRequestException;
import com.internconnect.exception.DuplicateResourceException;
import com.internconnect.repository.*;
import com.internconnect.security.CustomUserDetailsService;
import com.internconnect.service.AuthService;
import com.internconnect.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthServiceImpl implements AuthService {

    private final StudentRepository studentRepository;
    private final CompanyRepository companyRepository;
    private final AdminRepository adminRepository;
    private final PlacementOfficerRepository officerRepository;
    private final CollegeRepository collegeRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    @Override
    public AuthResponse registerStudent(StudentRegisterRequest request) {
        if (emailExistsAcrossAll(request.getEmail())) {
            throw new DuplicateResourceException("Email already in use: " + request.getEmail());
        }

        if (request.getCollege() == null || request.getCollege().isBlank() ||
                collegeRepository.findByNameIgnoreCase(request.getCollege().trim()).filter(College::isActive).isEmpty()) {
            throw new BadRequestException("Please select an active college created by the Admin");
        }

        Student student = Student.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .college(request.getCollege().trim())
                .branch(request.getBranch())
                .year(request.getYear())
                .cgpa(request.getCgpa())
                .role(Role.STUDENT)
                .enabled(true)
                .build();

        student = studentRepository.save(student);
        log.info("New student registered: {}", student.getEmail());

        UserDetails userDetails = userDetailsService.loadUserByUsername(student.getEmail());
        String token = jwtUtil.generateToken(userDetails);

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .email(student.getEmail())
                .role(student.getRole().name())
                .userId(student.getId())
                .name(student.getFirstName() + " " + student.getLastName())
                .build();
    }

    @Override
    public AuthResponse registerCompany(CompanyRegisterRequest request) {
        if (emailExistsAcrossAll(request.getEmail())) {
            throw new DuplicateResourceException("Email already in use: " + request.getEmail());
        }

        Company company = Company.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .address(request.getAddress())
                .website(request.getWebsite())
                .industry(request.getIndustry())
                .description(request.getDescription())
                .role(Role.COMPANY)
                .status(Company.CompanyStatus.PENDING)
                .enabled(true)
                .build();

        company = companyRepository.save(company);
        log.info("New company registered: {}", company.getEmail());

        UserDetails userDetails = userDetailsService.loadUserByUsername(company.getEmail());
        String token = jwtUtil.generateToken(userDetails);

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .email(company.getEmail())
                .role(company.getRole().name())
                .userId(company.getId())
                .name(company.getName())
                .build();
    }
    @Override
    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String token = jwtUtil.generateToken(userDetails);

            // Extract role from request OR dynamically fallback to GrantedAuthorities
            String role = request.getRole();
            if (role == null || role.isBlank()) {
                role = userDetails.getAuthorities().stream()
                        .findFirst()
                        .map(a -> a.getAuthority().replace("ROLE_", ""))
                        .orElse("STUDENT");
            } else {
                role = role.toUpperCase();
            }

            Long userId = null;
            String name = "";
            String college = null;

            switch (role) {
                case "STUDENT" -> {
                    var student = studentRepository.findByEmail(request.getEmail())
                            .orElseThrow(() -> new BadRequestException("Student not found"));
                    userId = student.getId();
                    name = student.getFirstName() + " " + student.getLastName();
                    college = student.getCollege();
                }
                case "COMPANY" -> {
                    var company = companyRepository.findByEmail(request.getEmail())
                            .orElseThrow(() -> new BadRequestException("Company not found"));
                    userId = company.getId();
                    name = company.getName();
                }
                case "PLACEMENT_OFFICER", "OFFICER" -> {
                    var officer = officerRepository.findByEmail(request.getEmail())
                            .orElseThrow(() -> new BadRequestException("Officer not found"));
                    userId = officer.getId();
                    name = officer.getFirstName() + " " + officer.getLastName();
                    college = officer.getCollege();
                }
                case "ADMIN" -> {
                    var admin = adminRepository.findByEmail(request.getEmail())
                            .orElseThrow(() -> new BadRequestException("Admin not found"));
                    userId = admin.getId();
                    name = admin.getFirstName() + " " + admin.getLastName();
                }
                default -> throw new BadRequestException("Invalid role: " + role);
            }

            return AuthResponse.builder()
                    .token(token)
                    .type("Bearer")
                    .email(request.getEmail())
                    .role(role)
                    .userId(userId)
                    .name(name)
                    .college(college)
                    .build();
        } catch (Exception ex) {
            log.error("Login failed for {}: {}", request.getEmail(), ex.getMessage());
            throw ex;
        }
    }




    private boolean emailExistsAcrossAll(String email) {
        return studentRepository.existsByEmail(email)
                || companyRepository.existsByEmail(email)
                || officerRepository.existsByEmail(email)
                || adminRepository.existsByEmail(email);
    }
}

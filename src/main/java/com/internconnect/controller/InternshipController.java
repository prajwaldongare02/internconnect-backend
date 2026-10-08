package com.internconnect.controller;

import com.internconnect.dto.ApiResponse;
import com.internconnect.dto.InternshipResponse;
import com.internconnect.repository.StudentRepository;
import com.internconnect.repository.CompanyRepository;
import com.internconnect.repository.CollegeRepository;
import com.internconnect.service.InternshipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/internships")
@RequiredArgsConstructor
public class InternshipController {

    private final InternshipService internshipService;
    private final StudentRepository studentRepository;
    private final CollegeRepository collegeRepository;
    private final CompanyRepository companyRepository;

    /**
     * Student marketplace only. Companies, Placement Officers and Admins
     * use their own role-specific internship views.
     */
    @GetMapping("/public/all")
    public ResponseEntity<ApiResponse<List<InternshipResponse>>> getAllOpenInternships(Authentication authentication) {
        String studentCollege = requireStudentCollege(authentication);
        List<InternshipResponse> internships = internshipService.getVisibleInternships(studentCollege);
        return ResponseEntity.ok(ApiResponse.success("Internships fetched", internships));
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<ApiResponse<InternshipResponse>> getInternshipById(@PathVariable Long id, Authentication authentication) {
        String studentCollege = requireStudentCollege(authentication);
        InternshipResponse internship = internshipService.getInternshipById(id);
        boolean companyApproved = internship.getCompanyId() != null
                && companyRepository.findById(internship.getCompanyId())
                .map(c -> c.getStatus() == com.internconnect.entity.Company.CompanyStatus.APPROVED)
                .orElse(false);
        if (!companyApproved) {
            throw new AccessDeniedException("This internship is not available until the company is approved by the Admin");
        }
        boolean visible = internship.getApprovedColleges() != null
                && internship.getApprovedColleges().stream().anyMatch(c -> c.equalsIgnoreCase(studentCollege));
        if (!visible) {
            throw new AccessDeniedException("This internship has not been opened for your college");
        }
        return ResponseEntity.ok(ApiResponse.success("Internship fetched", internship));
    }

    @GetMapping("/public/search")
    public ResponseEntity<ApiResponse<List<InternshipResponse>>> searchInternships(
            @RequestParam String keyword, Authentication authentication) {
        String studentCollege = requireStudentCollege(authentication);
        List<InternshipResponse> internships = internshipService.searchInternships(keyword).stream()
                .filter(i -> i.getCompanyId() != null
                        && companyRepository.findById(i.getCompanyId())
                        .map(c -> c.getStatus() == com.internconnect.entity.Company.CompanyStatus.APPROVED)
                        .orElse(false))
                .filter(i -> i.getApprovedColleges() != null
                        && i.getApprovedColleges().stream().anyMatch(c -> c.equalsIgnoreCase(studentCollege)))
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Search results", internships));
    }

    private String requireStudentCollege(Authentication authentication) {
        if (authentication == null) {
            throw new AccessDeniedException("Login as a student to browse internships");
        }
        boolean isStudent = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_STUDENT"));
        if (!isStudent) {
            throw new AccessDeniedException("Only students can browse internships");
        }
        String college = studentRepository.findByEmail(authentication.getName())
                .map(com.internconnect.entity.Student::getCollege)
                .filter(c -> !c.isBlank())
                .orElseThrow(() -> new AccessDeniedException("Your student profile has no college assigned"));
        if (collegeRepository.findByNameIgnoreCase(college.trim()).filter(com.internconnect.entity.College::isActive).isEmpty()) {
            throw new AccessDeniedException("Your college is currently inactive. Contact the Admin.");
        }
        return college.trim();
    }
}

package com.internconnect.controller;

import com.internconnect.dto.ApiResponse;
import com.internconnect.dto.ApplicationResponse;
import com.internconnect.dto.InternshipRequest;
import com.internconnect.dto.InternshipResponse;
import com.internconnect.entity.Application;
import com.internconnect.entity.Company;
import com.internconnect.service.ApplicationService;
import com.internconnect.service.CompanyService;
import com.internconnect.service.InternshipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/company")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COMPANY')")
public class CompanyController {

    private final CompanyService companyService;
    private final InternshipService internshipService;
    private final ApplicationService applicationService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<Company>> getMyProfile(Authentication authentication) {
        Company company = companyService.getCompanyByEmail(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Profile fetched successfully", company));
    }

    @PostMapping("/internships")
    public ResponseEntity<ApiResponse<InternshipResponse>> createInternship(
            @Valid @RequestBody InternshipRequest request,
            Authentication authentication) {
        Company company = companyService.getCompanyByEmail(authentication.getName());
        InternshipResponse internship = internshipService.createInternship(company.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Internship created successfully", internship));
    }

    @GetMapping("/internships")
    public ResponseEntity<ApiResponse<List<InternshipResponse>>> getMyInternships(Authentication authentication) {
        Company company = companyService.getCompanyByEmail(authentication.getName());
        List<InternshipResponse> internships = internshipService.getInternshipsByCompany(company.getId());
        return ResponseEntity.ok(ApiResponse.success("Internships fetched successfully", internships));
    }

    @PutMapping("/internships/{id}")
    public ResponseEntity<ApiResponse<InternshipResponse>> updateInternship(
            @PathVariable Long id,
            @Valid @RequestBody InternshipRequest request) {
        InternshipResponse internship = internshipService.updateInternship(id, request);
        return ResponseEntity.ok(ApiResponse.success("Internship updated successfully", internship));
    }

    @DeleteMapping("/internships/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteInternship(@PathVariable Long id) {
        internshipService.deleteInternship(id);
        return ResponseEntity.ok(ApiResponse.success("Internship deleted successfully", null));
    }

    @GetMapping("/applications")
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> getApplications(Authentication authentication) {
        Company company = companyService.getCompanyByEmail(authentication.getName());
        List<ApplicationResponse> applications = applicationService.getApplicationsByCompany(company.getId());
        return ResponseEntity.ok(ApiResponse.success("Applications fetched successfully", applications));
    }

    @PatchMapping("/applications/{id}/status")
    public ResponseEntity<ApiResponse<ApplicationResponse>> updateApplicationStatus(
            @PathVariable Long id,
            @RequestParam Application.ApplicationStatus status,
            @RequestParam(required = false) String remarks,
            Authentication authentication) {
        // Company workflow intentionally supports only the two decisions
        // exposed in the UI: shortlist for the first round or reject.
        // This prevents ACCEPTED/other statuses from being created through
        // a direct API request after the Accept button was removed.
        if (status != Application.ApplicationStatus.SHORTLISTED
                && status != Application.ApplicationStatus.REJECTED) {
            throw new com.internconnect.exception.BadRequestException(
                    "Company can only shortlist an applicant for 1 round or reject the application");
        }

        Company company = companyService.getCompanyByEmail(authentication.getName());
        ApplicationResponse application = applicationService.updateApplicationStatusForCompany(id, company.getId(), status, remarks);
        return ResponseEntity.ok(ApiResponse.success("Application status updated", application));
    }
}

package com.internconnect.controller;

import com.internconnect.dto.ApiResponse;
import com.internconnect.dto.CollegeRequest;
import com.internconnect.dto.PlacementOfficerRequest;
import com.internconnect.entity.*;
import com.internconnect.service.ApplicationService;
import com.internconnect.service.CompanyService;
import com.internconnect.service.StudentService;
import com.internconnect.service.InternshipService;
import com.internconnect.dto.InternshipResponse;
import com.internconnect.repository.PlacementOfficerRepository;
import com.internconnect.repository.CollegeRepository;
import com.internconnect.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final StudentService studentService;
    private final InternshipService internshipService;
    private final CompanyService companyService;
    private final ApplicationService applicationService;
    private final PlacementOfficerRepository officerRepository;
    private final PasswordEncoder passwordEncoder;
    private final CollegeRepository collegeRepository;

    // ===== College Management =====

    @GetMapping("/colleges")
    public ResponseEntity<ApiResponse<List<College>>> getColleges() {
        return ResponseEntity.ok(ApiResponse.success("Colleges fetched", collegeRepository.findAll(org.springframework.data.domain.Sort.by("name"))));
    }

    @PostMapping("/colleges")
    public ResponseEntity<ApiResponse<College>> createCollege(@Valid @RequestBody CollegeRequest request) {
        String name = request.getName().trim();
        if (collegeRepository.existsByNameIgnoreCase(name)) {
            throw new com.internconnect.exception.DuplicateResourceException("College already exists: " + name);
        }
        College college = College.builder().name(name).active(true).build();
        return ResponseEntity.status(201).body(ApiResponse.success("College created", collegeRepository.save(college)));
    }

    @PutMapping("/colleges/{id}/status")
    public ResponseEntity<ApiResponse<College>> updateCollegeStatus(@PathVariable Long id, @RequestBody java.util.Map<String, Boolean> body) {
        College college = collegeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("College", "id", id));
        college.setActive(Boolean.TRUE.equals(body.get("active")));
        return ResponseEntity.ok(ApiResponse.success("College status updated", collegeRepository.save(college)));
    }

    @GetMapping("/placement-officers")
    public ResponseEntity<ApiResponse<List<PlacementOfficer>>> getPlacementOfficers() {
        return ResponseEntity.ok(ApiResponse.success("Placement officers fetched", officerRepository.findAll()));
    }

    @PostMapping("/placement-officers")
    public ResponseEntity<ApiResponse<PlacementOfficer>> createPlacementOfficer(@Valid @RequestBody PlacementOfficerRequest request) {
        String[] names = request.getFullName().trim().split("\\s+", 2);
        validateCollege(request.getCollege());
        PlacementOfficer officer = PlacementOfficer.builder()
                .firstName(names[0]).lastName(names.length > 1 ? names[1] : "Officer")
                .email(request.getEmail()).password(passwordEncoder.encode(request.getPassword()))
                .college(request.getCollege().trim()).role(Role.PLACEMENT_OFFICER)
                .enabled(request.getIsActive() == null || request.getIsActive()).build();
        return ResponseEntity.status(201).body(ApiResponse.success("Placement officer created", officerRepository.save(officer)));
    }

    @PutMapping("/placement-officers/{id}")
    public ResponseEntity<ApiResponse<PlacementOfficer>> updatePlacementOfficer(@PathVariable Long id, @Valid @RequestBody PlacementOfficerRequest request) {
        PlacementOfficer officer = officerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Placement officer", "id", id));
        String[] names = request.getFullName().trim().split("\\s+", 2);
        officer.setFirstName(names[0]); officer.setLastName(names.length > 1 ? names[1] : "Officer");
        validateCollege(request.getCollege());
        officer.setEmail(request.getEmail()); officer.setCollege(request.getCollege().trim());
        if (request.getPassword() != null && !request.getPassword().isBlank()) officer.setPassword(passwordEncoder.encode(request.getPassword()));
        return ResponseEntity.ok(ApiResponse.success("Placement officer updated", officerRepository.save(officer)));
    }

    @PutMapping("/placement-officers/{id}/status")
    public ResponseEntity<ApiResponse<PlacementOfficer>> updatePlacementOfficerStatus(@PathVariable Long id, @RequestBody java.util.Map<String, Boolean> request) {
        PlacementOfficer officer = officerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Placement officer", "id", id));
        officer.setEnabled(Boolean.TRUE.equals(request.get("isActive")));
        return ResponseEntity.ok(ApiResponse.success("Placement officer status updated", officerRepository.save(officer)));
    }

    @DeleteMapping("/placement-officers/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePlacementOfficer(@PathVariable Long id) {
        if (!officerRepository.existsById(id)) throw new ResourceNotFoundException("Placement officer", "id", id);
        officerRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Placement officer deleted", null));
    }

    // ===== Student Management =====

    @GetMapping("/students")
    public ResponseEntity<ApiResponse<List<Student>>> getAllStudents() {
        return ResponseEntity.ok(ApiResponse.success("Students fetched", studentService.getAllStudents()));
    }

    @DeleteMapping("/students/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.ok(ApiResponse.success("Student deleted", null));
    }

    // ===== Company Management =====

    @GetMapping("/companies")
    public ResponseEntity<ApiResponse<List<Company>>> getAllCompanies() {
        return ResponseEntity.ok(ApiResponse.success("Companies fetched", companyService.getAllCompanies()));
    }

    @GetMapping("/companies/pending")
    public ResponseEntity<ApiResponse<List<Company>>> getPendingCompanies() {
        List<Company> pending = companyService.getCompaniesByStatus(Company.CompanyStatus.PENDING);
        return ResponseEntity.ok(ApiResponse.success("Pending companies fetched", pending));
    }

    @PatchMapping("/companies/{id}/approve")
    public ResponseEntity<ApiResponse<Company>> approveCompany(@PathVariable Long id) {
        Company company = companyService.updateCompanyStatus(id, Company.CompanyStatus.APPROVED);
        return ResponseEntity.ok(ApiResponse.success("Company approved", company));
    }

    @PatchMapping("/companies/{id}/reject")
    public ResponseEntity<ApiResponse<Company>> rejectCompany(@PathVariable Long id) {
        Company company = companyService.updateCompanyStatus(id, Company.CompanyStatus.REJECTED);
        return ResponseEntity.ok(ApiResponse.success("Company rejected", company));
    }

    @PutMapping("/companies/{id}/status")
    public ResponseEntity<ApiResponse<Company>> updateCompanyStatus(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        Company.CompanyStatus status = Company.CompanyStatus.valueOf(body.get("status").equals("BLOCKED") ? "REJECTED" : body.get("status"));
        return ResponseEntity.ok(ApiResponse.success("Company status updated", companyService.updateCompanyStatus(id, status)));
    }

    @DeleteMapping("/companies/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCompany(@PathVariable Long id) {
        companyService.deleteCompany(id);
        return ResponseEntity.ok(ApiResponse.success("Company deleted", null));
    }

    // ===== Internship Management =====

    @GetMapping("/internships")
    public ResponseEntity<ApiResponse<List<InternshipResponse>>> getAllInternships() {
        return ResponseEntity.ok(ApiResponse.success("All internships fetched", internshipService.getAllInternshipsForAdmin()));
    }

    // ===== Application Management =====

    @GetMapping("/applications/internship/{internshipId}")
    public ResponseEntity<ApiResponse<List<com.internconnect.dto.ApplicationResponse>>> getApplicationsByInternship(
            @PathVariable Long internshipId) {
        List<com.internconnect.dto.ApplicationResponse> applications = applicationService.getApplicationsByInternship(internshipId);
        return ResponseEntity.ok(ApiResponse.success("Applications fetched", applications));
    }

    @GetMapping("/applications/student/{studentId}")
    public ResponseEntity<ApiResponse<List<com.internconnect.dto.ApplicationResponse>>> getApplicationsByStudent(
            @PathVariable Long studentId) {
        List<com.internconnect.dto.ApplicationResponse> applications = applicationService.getApplicationsByStudent(studentId);
        return ResponseEntity.ok(ApiResponse.success("Applications fetched", applications));
    }
    private void validateCollege(String collegeName) {
        if (collegeName == null || collegeName.isBlank() ||
                collegeRepository.findByNameIgnoreCase(collegeName.trim()).filter(College::isActive).isEmpty()) {
            throw new com.internconnect.exception.BadRequestException("Select an active college created by the Admin");
        }
    }

}

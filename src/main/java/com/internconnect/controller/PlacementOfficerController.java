package com.internconnect.controller;

import com.internconnect.dto.ApiResponse;
import com.internconnect.dto.ApplicationResponse;
import com.internconnect.dto.InternshipResponse;
import com.internconnect.entity.Company;
import com.internconnect.entity.Student;
import com.internconnect.entity.PlacementOfficer;
import com.internconnect.entity.CollegeInternshipApproval;
import com.internconnect.service.ApplicationService;
import com.internconnect.service.CompanyService;
import com.internconnect.service.InternshipService;
import com.internconnect.service.StudentService;
import com.internconnect.repository.PlacementOfficerRepository;
import com.internconnect.repository.CollegeRepository;
import com.internconnect.exception.ResourceNotFoundException;
import com.internconnect.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping("/api/officer")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PLACEMENT_OFFICER', 'ADMIN')")
public class PlacementOfficerController {

    private final StudentService studentService;
    private final CompanyService companyService;
    private final ApplicationService applicationService;
    private final InternshipService internshipService;
    private final PlacementOfficerRepository officerRepository;
    private final CollegeRepository collegeRepository;

    @GetMapping("/internships/pending")
    public ResponseEntity<ApiResponse<List<InternshipResponse>>> getPendingInternships(Authentication authentication) {
        PlacementOfficer officer = currentOfficer(authentication);
        List<InternshipResponse> pending = internshipService.getPendingForCollege(officer.getCollege());
        return ResponseEntity.ok(ApiResponse.success("Pending internships fetched", pending));
    }

    @GetMapping("/internships/approved")
    public ResponseEntity<ApiResponse<List<InternshipResponse>>> getApprovedInternships(Authentication authentication) {
        PlacementOfficer officer = currentOfficer(authentication);
        List<InternshipResponse> approved = internshipService.getApprovedForCollege(officer.getCollege());
        return ResponseEntity.ok(ApiResponse.success("Approved internships fetched", approved));
    }

    @PutMapping("/internships/{id}/approve")
    public ResponseEntity<ApiResponse<Void>> approveInternship(@PathVariable Long id, Authentication authentication) {
        PlacementOfficer officer = currentOfficer(authentication);
        internshipService.decideForCollege(id, officer.getCollege(), CollegeInternshipApproval.ApprovalStatus.APPROVED);
        return ResponseEntity.ok(ApiResponse.success("Internship approved for college", null));
    }

    @PutMapping("/internships/{id}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectInternship(@PathVariable Long id, Authentication authentication) {
        PlacementOfficer officer = currentOfficer(authentication);
        internshipService.decideForCollege(id, officer.getCollege(), CollegeInternshipApproval.ApprovalStatus.REJECTED);
        return ResponseEntity.ok(ApiResponse.success("Internship rejected for college", null));
    }

    @GetMapping("/students")
    public ResponseEntity<ApiResponse<List<Student>>> getAllStudents(Authentication authentication) {
        PlacementOfficer officer = currentOfficer(authentication);
        List<Student> students = studentService.getAllStudents().stream()
                .filter(student -> officer.getCollege() != null && officer.getCollege().equalsIgnoreCase(student.getCollege()))
                .toList();
        return ResponseEntity.ok(ApiResponse.success("College students fetched", students));
    }

    @GetMapping("/students/{id}")
    public ResponseEntity<ApiResponse<Student>> getStudentById(@PathVariable Long id, Authentication authentication) {
        PlacementOfficer officer = currentOfficer(authentication);
        Student student = studentService.getStudentById(id);
        if (student.getCollege() == null || !officer.getCollege().equalsIgnoreCase(student.getCollege().trim())) {
            throw new BadRequestException("You are not authorized to view students from another college");
        }
        return ResponseEntity.ok(ApiResponse.success("Student fetched", student));
    }

    @GetMapping("/companies")
    public ResponseEntity<ApiResponse<List<Company>>> getAllCompanies() {
        return ResponseEntity.ok(ApiResponse.success("Companies fetched", companyService.getAllCompanies()));
    }

    @GetMapping("/applications/student/{studentId}")
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> getApplicationsByStudent(
            @PathVariable Long studentId, Authentication authentication) {
        PlacementOfficer officer = currentOfficer(authentication);
        Student student = studentService.getStudentById(studentId);
        if (student.getCollege() == null || !officer.getCollege().equalsIgnoreCase(student.getCollege().trim())) {
            throw new BadRequestException("You are not authorized to view applications from another college");
        }
        List<ApplicationResponse> applications = applicationService.getApplicationsByStudent(studentId).stream()
                .filter(a -> a.getStatus() != com.internconnect.entity.Application.ApplicationStatus.WITHDRAWN.name())
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Applications fetched", applications));
    }

    @GetMapping("/applications")
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> getApplicationsForCollege(Authentication authentication) {
        PlacementOfficer officer = currentOfficer(authentication);
        List<ApplicationResponse> applications = applicationService.getApplicationsForCollege(officer.getCollege());
        return ResponseEntity.ok(ApiResponse.success("College student applications fetched", applications));
    }

    @GetMapping("/applications/internship/{internshipId}")
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> getApplicationsByInternship(
            @PathVariable Long internshipId, Authentication authentication) {
        PlacementOfficer officer = currentOfficer(authentication);
        // Keep the existing endpoint useful while enforcing the TPO's college boundary.
        boolean openedForCollege = internshipService.getApprovedForCollege(officer.getCollege()).stream()
                .anyMatch(i -> i.getId().equals(internshipId));
        if (!openedForCollege) {
            throw new com.internconnect.exception.BadRequestException("This internship is not opened for your college.");
        }
        List<ApplicationResponse> applications = applicationService.getApplicationsByInternship(internshipId).stream()
                .filter(a -> a.getStatus() != com.internconnect.entity.Application.ApplicationStatus.WITHDRAWN.name())
                .filter(a -> a.getCollege() != null && officer.getCollege().equalsIgnoreCase(a.getCollege().trim()))
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Applications fetched", applications));
    }

    @PatchMapping("/applications/{id}/status")
    public ResponseEntity<ApiResponse<ApplicationResponse>> updateStatus(
            @PathVariable Long id,
            @RequestParam com.internconnect.entity.Application.ApplicationStatus status,
            @RequestParam(required = false) String remarks,
            Authentication authentication) {
        PlacementOfficer officer = currentOfficer(authentication);
        ApplicationResponse application = applicationService.updateApplicationStatusForCollege(id, officer.getCollege(), status, remarks);
        return ResponseEntity.ok(ApiResponse.success("Status updated", application));
    }

    private PlacementOfficer currentOfficer(Authentication authentication) {
        PlacementOfficer officer = officerRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Placement officer", "email", authentication.getName()));
        if (officer.getCollege() == null || officer.getCollege().isBlank() ||
                collegeRepository.findByNameIgnoreCase(officer.getCollege().trim()).filter(com.internconnect.entity.College::isActive).isEmpty()) {
            throw new com.internconnect.exception.BadRequestException("Placement officer is not assigned to an active college. Ask Admin to assign a college.");
        }
        return officer;
    }
}

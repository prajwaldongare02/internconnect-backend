package com.internconnect.controller;

import com.internconnect.dto.ApiResponse;
import com.internconnect.dto.ApplicationRequest;
import com.internconnect.dto.ApplicationResponse;
import com.internconnect.entity.Student;
import com.internconnect.service.ApplicationService;
import com.internconnect.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/student")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentController {

    private final StudentService studentService;
    private final ApplicationService applicationService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<Student>> getMyProfile(Authentication authentication) {
        Student student = studentService.getStudentByEmail(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Profile fetched successfully", student));
    }

    @PutMapping("/profile/{id}")
    public ResponseEntity<ApiResponse<Student>> updateProfile(
            @PathVariable Long id,
            @RequestBody Student student) {
        Student updated = studentService.updateStudent(id, student);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updated));
    }

    @PostMapping(value = "/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ApplicationResponse>> applyForInternship(
            @RequestParam("internshipId") Long internshipId,
            @RequestParam(value = "coverLetter", required = false) String coverLetter,
            @RequestPart("resume") MultipartFile resume,
            Authentication authentication) {
        Student student = studentService.getStudentByEmail(authentication.getName());

        ApplicationRequest request = new ApplicationRequest();
        request.setInternshipId(internshipId);
        request.setCoverLetter(coverLetter);

        ApplicationResponse application = applicationService.applyForInternship(student.getId(), request, resume);
        return ResponseEntity.ok(ApiResponse.success("Application submitted successfully", application));
    }

    @GetMapping("/applications")
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> getMyApplications(Authentication authentication) {
        Student student = studentService.getStudentByEmail(authentication.getName());
        List<ApplicationResponse> applications = applicationService.getApplicationsByStudent(student.getId());
        return ResponseEntity.ok(ApiResponse.success("Applications fetched successfully", applications));
    }

    @DeleteMapping("/applications/{applicationId}/withdraw")
    public ResponseEntity<ApiResponse<Void>> withdrawApplication(
            @PathVariable Long applicationId,
            Authentication authentication) {
        Student student = studentService.getStudentByEmail(authentication.getName());
        applicationService.withdrawApplication(applicationId, student.getId());
        return ResponseEntity.ok(ApiResponse.success("Application withdrawn successfully", null));
    }
}

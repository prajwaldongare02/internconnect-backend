package com.internconnect.service.impl;

import com.internconnect.dto.ApplicationRequest;
import com.internconnect.dto.ApplicationResponse;
import com.internconnect.entity.Application;
import com.internconnect.entity.Internship;
import com.internconnect.entity.Student;
import com.internconnect.exception.BadRequestException;
import com.internconnect.exception.ResourceNotFoundException;
import com.internconnect.repository.ApplicationRepository;
import com.internconnect.repository.CollegeInternshipApprovalRepository;
import com.internconnect.repository.InternshipRepository;
import com.internconnect.repository.StudentRepository;
import com.internconnect.service.ApplicationService;
import com.internconnect.service.ResumeStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/**
 * Every public method here returns ApplicationResponse DTOs, built while
 * the transactional session is still open, rather than the Application
 * entity itself (whose `student` and `internship` associations are LAZY
 * and would throw LazyInitializationException if Jackson tried to read
 * them after the session closes — see InternshipServiceImpl for details).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final StudentRepository studentRepository;
    private final InternshipRepository internshipRepository;
    private final CollegeInternshipApprovalRepository approvalRepository;
    private final ResumeStorageService resumeStorageService;

    @Override
    public ApplicationResponse applyForInternship(Long studentId, ApplicationRequest request, MultipartFile resume) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", studentId));

        Internship internship = internshipRepository.findById(request.getInternshipId())
                .orElseThrow(() -> new ResourceNotFoundException("Internship", "id", request.getInternshipId()));

        // A withdrawn application is no longer an active application.
        // It remains in history, but the student is allowed to apply again.
        boolean hasActiveApplication = applicationRepository
                .findByStudentIdAndInternshipId(studentId, request.getInternshipId())
                .stream()
                .anyMatch(existing -> existing.getStatus() != Application.ApplicationStatus.WITHDRAWN);
        if (hasActiveApplication) {
            throw new BadRequestException("You have already applied for this internship");
        }

        if (internship.getStatus() != Internship.InternshipStatus.OPEN) {
            throw new BadRequestException("This internship is not accepting applications");
        }

        if (internship.getCompany() == null
                || internship.getCompany().getStatus() != com.internconnect.entity.Company.CompanyStatus.APPROVED) {
            throw new BadRequestException("This internship is not available because the company has not been approved by the Admin");
        }

        String studentCollege = student.getCollege();
        if (studentCollege == null || studentCollege.isBlank()
                || approvalRepository.findByInternshipIdAndCollege(request.getInternshipId(), studentCollege.trim())
                    .filter(a -> a.getStatus() == com.internconnect.entity.CollegeInternshipApproval.ApprovalStatus.APPROVED)
                    .isEmpty()) {
            throw new BadRequestException("This internship has not been opened for your college");
        }

        if (resume == null || resume.isEmpty()) {
            throw new BadRequestException("Resume PDF is required to apply");
        }

        // Validate the application rules first. Only after the application is
        // known to be valid do we persist the selected resume locally.
        String resumeUrl = resumeStorageService.store(resume);

        Application application = Application.builder()
                .student(student)
                .internship(internship)
                .coverLetter(request.getCoverLetter())
                .resumeUrl(resumeUrl)
                .status(Application.ApplicationStatus.PENDING)
                .build();

        Application saved = applicationRepository.save(application);
        return toResponse(saved, student, internship);
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationById(Long id) {
        return toResponse(findEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicationsByStudent(Long studentId) {
        return applicationRepository.findByStudentId(studentId).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicationsByInternship(Long internshipId) {
        return applicationRepository.findByInternshipId(internshipId).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicationsByCompany(Long companyId) {
        return applicationRepository.findByInternshipCompanyId(companyId).stream()
                .filter(a -> a.getStatus() != Application.ApplicationStatus.WITHDRAWN)
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicationsForCollege(String college) {
        if (college == null || college.isBlank()) {
            return List.of();
        }

        // A TPO may see applications only for internships that this TPO
        // explicitly opened for their own college. This keeps the college
        // boundary intact even when the same company internship is opened
        // for several different colleges.
        List<Long> approvedInternshipIds = approvalRepository
                .findByCollegeAndStatus(college.trim(), com.internconnect.entity.CollegeInternshipApproval.ApprovalStatus.APPROVED)
                .stream()
                .map(a -> a.getInternship().getId())
                .distinct()
                .toList();

        if (approvedInternshipIds.isEmpty()) {
            return List.of();
        }

        String normalizedCollege = college.trim();
        return approvedInternshipIds.stream()
                .flatMap(id -> applicationRepository.findByInternshipId(id).stream())
                .filter(a -> a.getStatus() != Application.ApplicationStatus.WITHDRAWN)
                .filter(a -> a.getStudent() != null
                        && a.getStudent().getCollege() != null
                        && normalizedCollege.equalsIgnoreCase(a.getStudent().getCollege().trim()))
                .map(this::toResponse)
                .toList();
    }

    @Override
    public ApplicationResponse updateApplicationStatus(Long id, Application.ApplicationStatus status, String remarks) {
        Application application = findEntity(id);
        if (application.getStatus() == Application.ApplicationStatus.WITHDRAWN) {
            throw new BadRequestException("This application has been withdrawn and can no longer be updated");
        }
        application.setStatus(status);
        if (remarks != null && !remarks.isBlank()) {
            application.setCompanyRemarks(remarks);
        }
        return toResponse(applicationRepository.save(application));
    }

    @Override
    public ApplicationResponse updateApplicationStatusForCompany(Long id, Long companyId, Application.ApplicationStatus status, String remarks) {
        Application application = findEntity(id);
        if (application.getInternship() == null || application.getInternship().getCompany() == null
                || !application.getInternship().getCompany().getId().equals(companyId)) {
            throw new BadRequestException("You are not authorized to update this application");
        }
        if (application.getStatus() == Application.ApplicationStatus.WITHDRAWN) {
            throw new BadRequestException("This application has been withdrawn and can no longer be updated");
        }
        if (status != Application.ApplicationStatus.SHORTLISTED
                && status != Application.ApplicationStatus.REJECTED) {
            throw new BadRequestException("Company can only shortlist an applicant for 1 round or reject the application");
        }
        application.setStatus(status);
        if (remarks != null && !remarks.isBlank()) {
            application.setCompanyRemarks(remarks);
        }
        return toResponse(applicationRepository.save(application));
    }

    @Override
    public ApplicationResponse updateApplicationStatusForCollege(Long id, String college, Application.ApplicationStatus status, String remarks) {
        Application application = findEntity(id);
        String normalizedCollege = college == null ? "" : college.trim();
        if (normalizedCollege.isBlank()
                || application.getStudent() == null
                || application.getStudent().getCollege() == null
                || !normalizedCollege.equalsIgnoreCase(application.getStudent().getCollege().trim())) {
            throw new BadRequestException("You are not authorized to update an application from another college");
        }
        Long internshipId = application.getInternship() != null ? application.getInternship().getId() : null;
        if (internshipId == null
                || approvalRepository.findByInternshipIdAndCollege(internshipId, normalizedCollege)
                    .filter(a -> a.getStatus() == com.internconnect.entity.CollegeInternshipApproval.ApprovalStatus.APPROVED)
                    .isEmpty()) {
            throw new BadRequestException("This internship is not opened for your college");
        }
        if (application.getStatus() == Application.ApplicationStatus.WITHDRAWN) {
            throw new BadRequestException("This application has been withdrawn and can no longer be updated");
        }
        application.setStatus(status);
        if (remarks != null && !remarks.isBlank()) {
            application.setOfficerRemarks(remarks);
        }
        return toResponse(applicationRepository.save(application));
    }

    @Override
    public void withdrawApplication(Long id, Long studentId) {
        Application application = findEntity(id);
        if (!application.getStudent().getId().equals(studentId)) {
            throw new BadRequestException("You are not authorized to withdraw this application");
        }
        if (application.getStatus() == Application.ApplicationStatus.WITHDRAWN) {
            throw new BadRequestException("This application is already withdrawn");
        }
        if (application.getStatus() == Application.ApplicationStatus.SELECTED
                || application.getStatus() == Application.ApplicationStatus.ACCEPTED) {
            throw new BadRequestException("Cannot withdraw an accepted application");
        }
        application.setStatus(Application.ApplicationStatus.WITHDRAWN);
        applicationRepository.saveAndFlush(application);
    }

    private Application findEntity(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", id));
    }

    // ---------------- Mapping helpers ----------------

    private ApplicationResponse toResponse(Application a) {
        return toResponse(a, a.getStudent(), a.getInternship());
    }

    private ApplicationResponse toResponse(Application a, Student student, Internship internship) {
        var company = internship != null ? internship.getCompany() : null;
        return ApplicationResponse.builder()
                .id(a.getId())
                .internshipId(internship != null ? internship.getId() : null)
                .internshipTitle(internship != null ? internship.getTitle() : null)
                .companyName(company != null ? company.getName() : null)
                .location(internship != null ? internship.getLocation() : null)
                .type(internship != null ? internship.getMode() : null)
                .status(a.getStatus() != null ? a.getStatus().name() : null)
                .coverLetter(a.getCoverLetter())
                .resumeUrl(a.getResumeUrl())
                .companyRemarks(a.getCompanyRemarks())
                .officerRemarks(a.getOfficerRemarks())
                .appliedAt(a.getAppliedAt() != null ? a.getAppliedAt().format(DateTimeFormatter.ISO_LOCAL_DATE) : null)
                .studentId(student != null ? student.getId() : null)
                .studentName(student != null ? (student.getFirstName() + " " + (student.getLastName() != null ? student.getLastName() : "")).trim() : null)
                .studentEmail(student != null ? student.getEmail() : null)
                .department(student != null ? student.getBranch() : null)
                .college(student != null ? student.getCollege() : null)
                .build();
    }
}

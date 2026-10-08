package com.internconnect.service;

import com.internconnect.dto.ApplicationRequest;
import com.internconnect.dto.ApplicationResponse;
import com.internconnect.entity.Application;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface ApplicationService {
    ApplicationResponse applyForInternship(Long studentId, ApplicationRequest request, MultipartFile resume);
    ApplicationResponse getApplicationById(Long id);
    List<ApplicationResponse> getApplicationsByStudent(Long studentId);
    List<ApplicationResponse> getApplicationsByInternship(Long internshipId);
    List<ApplicationResponse> getApplicationsByCompany(Long companyId);
    List<ApplicationResponse> getApplicationsForCollege(String college);
    ApplicationResponse updateApplicationStatus(Long id, Application.ApplicationStatus status, String remarks);
    ApplicationResponse updateApplicationStatusForCompany(Long id, Long companyId, Application.ApplicationStatus status, String remarks);
    ApplicationResponse updateApplicationStatusForCollege(Long id, String college, Application.ApplicationStatus status, String remarks);
    void withdrawApplication(Long id, Long studentId);
}

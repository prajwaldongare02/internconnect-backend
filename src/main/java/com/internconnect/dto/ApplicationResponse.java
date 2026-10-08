package com.internconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Flat, frontend-ready representation of an Application.
 * See InternshipResponse for why this must be built inside a
 * @Transactional method rather than serializing the entity directly.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationResponse {
    private Long id;

    private Long internshipId;
    private String internshipTitle;
    private String companyName;
    private String location;
    private String type;

    private String status;
    private String coverLetter;
    private String resumeUrl;
    private String companyRemarks;
    private String officerRemarks;
    private String appliedAt;

    private Long studentId;
    private String studentName;
    private String studentEmail;
    private String department;
    private String college;
}

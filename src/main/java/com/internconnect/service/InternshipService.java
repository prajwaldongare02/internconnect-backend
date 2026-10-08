package com.internconnect.service;

import com.internconnect.dto.InternshipRequest;
import com.internconnect.dto.InternshipResponse;
import com.internconnect.entity.CollegeInternshipApproval;

import java.util.List;

public interface InternshipService {
    InternshipResponse createInternship(Long companyId, InternshipRequest request);
    InternshipResponse getInternshipById(Long id);

    /**
     * Internships visible to the caller.
     * - studentCollege == null/blank: every OPEN internship (used by
     *   non-student roles that don't need college gating).
     * - studentCollege set: only OPEN internships a Placement Officer
     *   has approved for that specific college.
     */
    List<InternshipResponse> getVisibleInternships(String studentCollege);

    List<InternshipResponse> getInternshipsByCompany(Long companyId);
    List<InternshipResponse> getAllInternshipsForAdmin();
    InternshipResponse updateInternship(Long id, InternshipRequest request);
    void deleteInternship(Long id);
    List<InternshipResponse> searchInternships(String keyword);

    // Placement Officer college-approval workflow
    List<InternshipResponse> getPendingForCollege(String college);
    List<InternshipResponse> getApprovedForCollege(String college);
    void decideForCollege(Long internshipId, String college, CollegeInternshipApproval.ApprovalStatus status);
}

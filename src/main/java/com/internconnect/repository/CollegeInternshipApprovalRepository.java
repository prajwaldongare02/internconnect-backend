package com.internconnect.repository;

import com.internconnect.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface CollegeInternshipApprovalRepository extends JpaRepository<CollegeInternshipApproval, Long> {
    Optional<CollegeInternshipApproval> findByInternshipIdAndCollege(Long internshipId, String college);
    List<CollegeInternshipApproval> findByCollegeAndStatus(String college, CollegeInternshipApproval.ApprovalStatus status);
    List<CollegeInternshipApproval> findByCollege(String college);
    List<CollegeInternshipApproval> findByInternshipIdAndStatus(Long internshipId, CollegeInternshipApproval.ApprovalStatus status);
}

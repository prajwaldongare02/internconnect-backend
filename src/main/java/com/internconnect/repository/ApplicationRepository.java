package com.internconnect.repository;

import com.internconnect.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByStudentId(Long studentId);
    List<Application> findByInternshipId(Long internshipId);
    List<Application> findByStatus(Application.ApplicationStatus status);
    List<Application> findByStudentIdAndInternshipId(Long studentId, Long internshipId);
    boolean existsByStudentIdAndInternshipId(Long studentId, Long internshipId);
    List<Application> findByInternshipCompanyId(Long companyId);
    long countByInternshipId(Long internshipId);
    long countByInternshipIdAndStatusNot(Long internshipId, Application.ApplicationStatus status);
}

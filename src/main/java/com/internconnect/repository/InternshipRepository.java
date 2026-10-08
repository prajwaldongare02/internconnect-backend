package com.internconnect.repository;

import com.internconnect.entity.Internship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InternshipRepository extends JpaRepository<Internship, Long> {
    List<Internship> findByCompanyId(Long companyId);
    List<Internship> findByStatus(Internship.InternshipStatus status);
    List<Internship> findByTitleContainingIgnoreCase(String keyword);
    List<Internship> findByLocationContainingIgnoreCaseOrTitleContainingIgnoreCase(String location, String title);
}

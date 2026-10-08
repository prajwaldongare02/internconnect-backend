package com.internconnect.repository;

import com.internconnect.entity.College;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CollegeRepository extends JpaRepository<College, Long> {
    Optional<College> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
    List<College> findByActiveTrueOrderByNameAsc();
}

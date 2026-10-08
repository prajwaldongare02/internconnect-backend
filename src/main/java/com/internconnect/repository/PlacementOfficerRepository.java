package com.internconnect.repository;

import com.internconnect.entity.PlacementOfficer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlacementOfficerRepository extends JpaRepository<PlacementOfficer, Long> {
    Optional<PlacementOfficer> findByEmail(String email);
    boolean existsByEmail(String email);
}

package com.internconnect.config;

import com.internconnect.entity.Admin;
import com.internconnect.entity.PlacementOfficer;
import com.internconnect.entity.Role;
import com.internconnect.repository.AdminRepository;
import com.internconnect.repository.PlacementOfficerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final PlacementOfficerRepository officerRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedAdmin();
        removeOldDemoOfficer();
    }

    private void seedAdmin() {
        if (!adminRepository.existsByEmail("admin@internconnect.com")) {
            Admin admin = Admin.builder()
                    .firstName("Super")
                    .lastName("Admin")
                    .email("admin@internconnect.com")
                    .password(passwordEncoder.encode("Admin@123"))
                    .adminCode("IC-ADMIN-001")
                    .role(Role.ADMIN)
                    .enabled(true)
                    .build();
            adminRepository.save(admin);
            log.info("✅ Default Admin seeded: admin@internconnect.com / Admin@123");
        }
    }

    /**
     * Older versions shipped with a fake ABC college/TPO. Remove that demo
     * account so colleges and TPO assignments are created by the Admin.
     */
    private void removeOldDemoOfficer() {
        officerRepository.findByEmail("officer@internconnect.com")
                .ifPresent(officer -> {
                    officerRepository.delete(officer);
                    log.info("Removed old demo Placement Officer and ABC college assignment");
                });
    }
}

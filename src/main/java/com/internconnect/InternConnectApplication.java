package com.internconnect;

import com.internconnect.entity.Admin; // Import your Admin entity class
import com.internconnect.entity.Role;
import com.internconnect.repository.AdminRepository; // Import your Admin repository interface
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@SpringBootApplication
public class InternConnectApplication {

    public static void main(String[] args) {
        SpringApplication.run(InternConnectApplication.class, args);
    }

    @Bean
    public CommandLineRunner initAdminData(AdminRepository adminRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            Admin admin = adminRepository.findByEmail("admin@internconnect.com")
                    .orElseGet(Admin::new);

            admin.setAdminCode("IC-ADMIN-001");
            admin.setFirstName("Super");
            admin.setLastName("Admin");
            admin.setEmail("admin@internconnect.com");
            admin.setPassword(passwordEncoder.encode("Admin@12345")); // Always re-hash password
            admin.setEnabled(true);
            admin.setRole(Role.ADMIN); // Use your Role enum/class

            if (admin.getCreatedAt() == null) {
                admin.setCreatedAt(LocalDateTime.now());
            }

            adminRepository.save(admin);
            System.out.println(">>> ADMIN USER UPDATED/CREATED SUCCESSFULLY WITH PASSWORD: Admin@12345 <<<");
        };
    }
}
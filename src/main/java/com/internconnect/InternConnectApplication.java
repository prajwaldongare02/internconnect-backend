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
            // Check if admin already exists to prevent duplication
            if (!adminRepository.existsByEmail("admin@internconnect.com")) {
                Admin admin = new Admin();
                admin.setAdminCode("IC-ADMIN-001");
                admin.setFirstName("Super");
                admin.setLastName("Admin");
                admin.setEmail("admin@internconnect.com");
                admin.setPassword(passwordEncoder.encode("Admin@12345")); // Encrypt password
                admin.setEnabled(true);
                // Import the Role enum if needed: import com.internconnect.entity.Role;
                admin.setRole(Role.ADMIN);
                admin.setCreatedAt(LocalDateTime.now());

                adminRepository.save(admin);
                System.out.println("Default Admin user created successfully in Railway MySQL!");
            }
        };
    }
}
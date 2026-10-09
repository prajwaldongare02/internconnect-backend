package com.internconnect.security;

import com.internconnect.entity.*;
import com.internconnect.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final StudentRepository studentRepository;
    private final CompanyRepository companyRepository;
    private final PlacementOfficerRepository placementOfficerRepository;
    private final AdminRepository adminRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        System.out.println(">>> AUTH LOOKUP FOR EMAIL: " + email + " <<<");

        // Try Student
        var student = studentRepository.findByEmail(email);
        if (student.isPresent()) {
            return buildUserDetails(student.get().getEmail(),
                    student.get().getPassword(),
                    student.get().getRole().name(),
                    student.get().isEnabled());
        }

        // Try Company
        var company = companyRepository.findByEmail(email);
        if (company.isPresent()) {
            return buildUserDetails(company.get().getEmail(),
                    company.get().getPassword(),
                    company.get().getRole().name(),
                    company.get().isEnabled());
        }

        // Try Placement Officer
        var officer = placementOfficerRepository.findByEmail(email);
        if (officer.isPresent()) {
            return buildUserDetails(officer.get().getEmail(),
                    officer.get().getPassword(),
                    officer.get().getRole().name(),
                    officer.get().isEnabled());
        }

        // Try Admin
        var admin = adminRepository.findByEmail(email);
        if (admin.isPresent()) {
            System.out.println(">>> ADMIN RECORD FOUND IN DB FOR: " + email + " <<<");
            return buildUserDetails(admin.get().getEmail(),
                    admin.get().getPassword(),
                    admin.get().getRole().name(),
                    admin.get().isEnabled());
        }

        throw new UsernameNotFoundException("User not found with email: " + email);
    }

    private UserDetails buildUserDetails(String email, String password, String role, boolean enabled) {
        // Strip "ROLE_" prefix if role already contains it to prevent "ROLE_ROLE_ADMIN"
        String roleName = role.startsWith("ROLE_") ? role : "ROLE_" + role;

        return User.builder()
                .username(email)
                .password(password)
                .authorities(List.of(new SimpleGrantedAuthority(roleName)))
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(!enabled)
                .build();
    }
}
package com.internconnect.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "placement_officers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlacementOfficer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "First name is required")
    @Column(nullable = false)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Column(nullable = false)
    private String lastName;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank(message = "Password is required")
    @Column(nullable = false)
    private String password;

    private String phone;
    private String employeeId;
    private String department;
    private String designation;
    private String college;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.PLACEMENT_OFFICER;

    @Column(nullable = false)
    private boolean enabled = true;

    @JsonIgnore
    public String getPassword() { return password; }

    @JsonProperty("fullName")
    public String getFullName() { return (firstName + " " + lastName).trim(); }

    @JsonProperty("isActive")
    public boolean isActive() { return enabled; }

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

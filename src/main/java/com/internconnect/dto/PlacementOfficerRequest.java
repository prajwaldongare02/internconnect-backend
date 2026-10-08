package com.internconnect.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PlacementOfficerRequest {
    @NotBlank private String fullName;
    @NotBlank @Email private String email;
    @NotBlank private String college;
    private String password;
    private Boolean isActive;
}

package com.internconnect.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CollegeRequest {
    @NotBlank(message = "College name is required")
    private String name;
}

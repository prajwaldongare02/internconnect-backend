package com.internconnect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class InternshipRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    private String requirements;
    private String responsibilities;
    private String skills;
    private String perks;
    private String location;
    private String mode;
    private Integer duration;
    private BigDecimal stipend;
    private String stipendCurrency;
    private Integer openings;

    @NotNull(message = "Application deadline is required")
    private LocalDate applicationDeadline;

    private LocalDate startDate;
    private LocalDate endDate;
}

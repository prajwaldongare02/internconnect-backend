package com.internconnect.dto;

import lombok.Data;

@Data
public class ApplicationRequest {
    private Long internshipId;
    private String coverLetter;
    private String resumeUrl;
}

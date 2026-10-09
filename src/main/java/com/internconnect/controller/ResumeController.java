package com.internconnect.controller;

import com.internconnect.dto.ApiResponse;
import com.internconnect.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final CloudinaryService cloudinaryService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadResume(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Please select a file to upload"));
            }

            String resumeUrl = cloudinaryService.uploadResume(file);
            return ResponseEntity.ok(ApiResponse.success("Upload successful", Map.of("url", resumeUrl)));
        } catch (Exception e) {
            e.printStackTrace(); // Prints stack trace to Railway logs
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Cloudinary Upload Failed: " + e.getMessage()));
        }
    }
}
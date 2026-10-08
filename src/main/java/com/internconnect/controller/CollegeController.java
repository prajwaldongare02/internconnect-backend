package com.internconnect.controller;

import com.internconnect.dto.ApiResponse;
import com.internconnect.entity.College;
import com.internconnect.repository.CollegeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/colleges")
@RequiredArgsConstructor
public class CollegeController {
    private final CollegeRepository collegeRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<College>>> getActiveColleges() {
        return ResponseEntity.ok(ApiResponse.success("Active colleges fetched", collegeRepository.findByActiveTrueOrderByNameAsc()));
    }
}

package com.internconnect.service;

import org.springframework.web.multipart.MultipartFile;

public interface ResumeStorageService {
    String store(MultipartFile file);
}

package com.internconnect.service.impl;

import com.internconnect.exception.BadRequestException;
import com.internconnect.service.ResumeStorageService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class LocalResumeStorageService implements ResumeStorageService {

    private static final long MAX_SIZE = 5 * 1024 * 1024;
    private final Path uploadDirectory = Paths.get("uploads", "resumes").toAbsolutePath().normalize();

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Resume PDF is required to apply");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BadRequestException("Resume must be 5 MB or smaller");
        }

        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "resume.pdf" : file.getOriginalFilename());
        String contentType = file.getContentType();
        boolean pdfType = "application/pdf".equalsIgnoreCase(contentType);
        boolean pdfExtension = originalName.toLowerCase().endsWith(".pdf");
        if (!pdfType && !pdfExtension) {
            throw new BadRequestException("Only PDF resumes are allowed");
        }

        try {
            Files.createDirectories(uploadDirectory);
            String storedName = UUID.randomUUID() + ".pdf";
            Path target = uploadDirectory.resolve(storedName).normalize();
            if (!target.getParent().equals(uploadDirectory)) {
                throw new BadRequestException("Invalid resume filename");
            }
            Files.copy(file.getInputStream(), target);
            return "/api/resumes/" + storedName;
        } catch (IOException ex) {
            throw new BadRequestException("Could not save the resume. Please try again");
        }
    }
}

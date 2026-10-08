package com.internconnect.service.impl;

import com.internconnect.dto.InternshipRequest;
import com.internconnect.dto.InternshipResponse;
import com.internconnect.entity.CollegeInternshipApproval;
import com.internconnect.entity.Company;
import com.internconnect.entity.Internship;
import com.internconnect.exception.ResourceNotFoundException;
import com.internconnect.repository.ApplicationRepository;
import com.internconnect.repository.CollegeInternshipApprovalRepository;
import com.internconnect.repository.CompanyRepository;
import com.internconnect.repository.InternshipRepository;
import com.internconnect.service.InternshipService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * IMPORTANT: every public method here builds and returns InternshipResponse
 * DTOs rather than Internship entities. spring.jpa.open-in-view is disabled
 * for this project, so the Hibernate session is closed the moment this
 * @Transactional method returns — any lazy field (e.g. Internship#company)
 * touched later, during Jackson serialization in the web layer, throws
 * LazyInitializationException ("no Session"). Building the DTO in here,
 * while the session is still open, avoids that entirely.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class InternshipServiceImpl implements InternshipService {

    private final InternshipRepository internshipRepository;
    private final CompanyRepository companyRepository;
    private final ApplicationRepository applicationRepository;
    private final CollegeInternshipApprovalRepository approvalRepository;

    @Override
    public InternshipResponse createInternship(Long companyId, InternshipRequest request) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company", "id", companyId));

        if (company.getStatus() != Company.CompanyStatus.APPROVED) {
            throw new com.internconnect.exception.BadRequestException(
                    "Your company account must be approved by the Admin before posting an internship");
        }

        Internship internship = Internship.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .requirements(request.getRequirements())
                .responsibilities(request.getResponsibilities())
                .skills(request.getSkills())
                .perks(request.getPerks())
                .location(request.getLocation())
                .mode(request.getMode())
                .duration(request.getDuration())
                .stipend(request.getStipend())
                .stipendCurrency(request.getStipendCurrency())
                .openings(request.getOpenings())
                .applicationDeadline(request.getApplicationDeadline())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(Internship.InternshipStatus.OPEN)
                .company(company)
                .build();

        Internship saved = internshipRepository.save(internship);
        // company is already the object we just loaded (not a proxy), so
        // this is safe to map immediately.
        return toResponse(saved, company);
    }

    @Override
    @Transactional(readOnly = true)
    public InternshipResponse getInternshipById(Long id) {
        Internship internship = internshipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Internship", "id", id));
        return toResponse(internship);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InternshipResponse> getVisibleInternships(String studentCollege) {
        List<Internship> internships;
        if (studentCollege == null || studentCollege.isBlank()) {
            internships = internshipRepository.findByStatus(Internship.InternshipStatus.OPEN);
        } else {
            List<Long> approvedIds = approvalRepository
                    .findByCollegeAndStatus(studentCollege, CollegeInternshipApproval.ApprovalStatus.APPROVED)
                    .stream()
                    .map(a -> a.getInternship().getId())
                    .toList();
            internships = approvedIds.isEmpty()
                    ? Collections.emptyList()
                    : internshipRepository.findAllById(approvedIds).stream()
                        .filter(i -> i.getStatus() == Internship.InternshipStatus.OPEN)
                        .filter(i -> i.getCompany() != null && i.getCompany().getStatus() == Company.CompanyStatus.APPROVED)
                        .toList();
        }
        return internships.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InternshipResponse> getInternshipsByCompany(Long companyId) {
        return internshipRepository.findByCompanyId(companyId).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InternshipResponse> getAllInternshipsForAdmin() {
        return internshipRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public InternshipResponse updateInternship(Long id, InternshipRequest request) {
        Internship existing = internshipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Internship", "id", id));
        existing.setTitle(request.getTitle());
        existing.setDescription(request.getDescription());
        existing.setRequirements(request.getRequirements());
        existing.setResponsibilities(request.getResponsibilities());
        existing.setSkills(request.getSkills());
        existing.setPerks(request.getPerks());
        existing.setLocation(request.getLocation());
        existing.setMode(request.getMode());
        existing.setDuration(request.getDuration());
        existing.setStipend(request.getStipend());
        existing.setStipendCurrency(request.getStipendCurrency());
        existing.setOpenings(request.getOpenings());
        existing.setApplicationDeadline(request.getApplicationDeadline());
        existing.setStartDate(request.getStartDate());
        existing.setEndDate(request.getEndDate());
        return toResponse(internshipRepository.save(existing));
    }

    @Override
    public void deleteInternship(Long id) {
        if (!internshipRepository.existsById(id)) {
            throw new ResourceNotFoundException("Internship", "id", id);
        }
        internshipRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InternshipResponse> searchInternships(String keyword) {
        return internshipRepository.findByTitleContainingIgnoreCase(keyword).stream().map(this::toResponse).toList();
    }

    // ---------------- Placement Officer college-approval workflow ----------------

    @Override
    @Transactional(readOnly = true)
    public List<InternshipResponse> getPendingForCollege(String college) {
        Set<Long> decided = approvalRepository.findByCollege(college).stream()
                .map(a -> a.getInternship().getId())
                .collect(Collectors.toSet());
        return internshipRepository.findByStatus(Internship.InternshipStatus.OPEN).stream()
                .filter(i -> i.getCompany() != null && i.getCompany().getStatus() == Company.CompanyStatus.APPROVED)
                .filter(i -> !decided.contains(i.getId()))
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InternshipResponse> getApprovedForCollege(String college) {
        return approvalRepository.findByCollegeAndStatus(college, CollegeInternshipApproval.ApprovalStatus.APPROVED)
                .stream()
                .filter(a -> a.getInternship() != null
                        && a.getInternship().getCompany() != null
                        && a.getInternship().getCompany().getStatus() == Company.CompanyStatus.APPROVED)
                .map(a -> {
                    InternshipResponse response = toResponse(a.getInternship());
                    response.setApprovedOn(a.getDecidedAt() != null
                            ? a.getDecidedAt().format(DateTimeFormatter.ISO_LOCAL_DATE)
                            : null);
                    return response;
                })
                .toList();
    }

    @Override
    public void decideForCollege(Long internshipId, String college, CollegeInternshipApproval.ApprovalStatus status) {
        if (college == null || college.isBlank()) {
            throw new IllegalArgumentException("Placement officer is not assigned to a college");
        }
        if (status == null) {
            throw new IllegalArgumentException("An approval decision is required");
        }

        Internship internship = internshipRepository.findById(internshipId)
                .orElseThrow(() -> new ResourceNotFoundException("Internship", "id", internshipId));

        // One decision per internship + college. Updating the same row makes
        // approve/reject operations idempotent and avoids duplicate-key errors.
        CollegeInternshipApproval approval = approvalRepository
                .findByInternshipIdAndCollege(internshipId, college.trim())
                .orElseGet(() -> CollegeInternshipApproval.builder()
                        .internship(internship)
                        .college(college.trim())
                        .build());
        approval.setStatus(status);
        approval.setDecidedAt(LocalDateTime.now());
        approvalRepository.saveAndFlush(approval);
    }

    // ---------------- Mapping helpers ----------------

    private InternshipResponse toResponse(Internship i) {
        return toResponse(i, i.getCompany());
    }

    private InternshipResponse toResponse(Internship i, Company company) {
        long applicants = applicationRepository.countByInternshipIdAndStatusNot(i.getId(), com.internconnect.entity.Application.ApplicationStatus.WITHDRAWN);

        return InternshipResponse.builder()
                .id(i.getId())
                .title(i.getTitle())
                .description(i.getDescription())
                .requirements(splitLines(i.getRequirements()))
                .responsibilities(splitLines(i.getResponsibilities()))
                .skills(splitCsv(i.getSkills()))
                .perks(splitLines(i.getPerks()))
                .location(i.getLocation())
                .type(i.getMode())
                .durationMonths(i.getDuration())
                .duration(formatDuration(i.getDuration()))
                .stipendAmount(i.getStipend())
                .stipend(formatStipend(i.getStipend(), i.getStipendCurrency()))
                .openings(i.getOpenings())
                .deadline(i.getApplicationDeadline())
                .startDate(i.getStartDate())
                .endDate(i.getEndDate())
                .status(i.getStatus() != null ? i.getStatus().name() : null)
                .companyId(company != null ? company.getId() : null)
                .companyName(company != null ? company.getName() : null)
                .companyAbout(company != null ? company.getDescription() : null)
                .companyLogoUrl(company != null ? company.getLogoUrl() : null)
                .applicants(applicants)
                .postedAt(i.getCreatedAt() != null ? i.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE) : null)
                .approvedColleges(approvedCollegesFor(i.getId()))
                .build();
    }

    private List<String> approvedCollegesFor(Long internshipId) {
        return approvalRepository.findByInternshipIdAndStatus(internshipId, CollegeInternshipApproval.ApprovalStatus.APPROVED)
                .stream()
                .map(CollegeInternshipApproval::getCollege)
                .toList();
    }

    private static List<String> splitLines(String value) {
        if (value == null || value.isBlank()) return new ArrayList<>();
        return Arrays.stream(value.split("\\r?\\n")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private static List<String> splitCsv(String value) {
        if (value == null || value.isBlank()) return new ArrayList<>();
        return Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private static String formatDuration(Integer months) {
        if (months == null) return null;
        return months + (months == 1 ? " month" : " months");
    }

    private static String formatStipend(BigDecimal amount, String currency) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) return "Unpaid";
        String symbol = "INR".equalsIgnoreCase(currency) || currency == null ? "\u20B9" : currency + " ";
        return symbol + amount.toBigInteger().toString() + "/mo";
    }
}

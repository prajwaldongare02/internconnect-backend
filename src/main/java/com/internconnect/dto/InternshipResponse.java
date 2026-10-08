package com.internconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Flat, frontend-ready representation of an Internship.
 *
 * IMPORTANT: this must always be built inside a @Transactional method
 * while the originating Hibernate session is still open, since it reads
 * the lazy `company` association. Never return the Internship entity
 * itself from a controller — with spring.jpa.open-in-view=false, Jackson
 * serializes the response after the session has already closed, which
 * throws LazyInitializationException ("no Session") for any unresolved
 * proxy (e.g. Internship#company, Application#student).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternshipResponse {
    private Long id;
    private String title;
    private String description;
    private java.util.List<String> requirements;
    private java.util.List<String> responsibilities;
    private java.util.List<String> skills;
    private java.util.List<String> perks;

    private String location;
    private String type; // employment type (Full-time / Part-time)
    private Integer durationMonths;
    private String duration; // display label e.g. "3 months"

    private BigDecimal stipendAmount;
    private String stipend; // display label e.g. "₹75,000/mo"

    private Integer openings;
    private LocalDate deadline;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;

    private Long companyId;
    private String companyName;
    private String companyAbout;
    private String companyLogoUrl;

    private long applicants;
    private String postedAt;

    /** Colleges whose Placement Officer has approved this internship. */
    private java.util.List<String> approvedColleges;

    /** Only set when returned from a college-scoped "approved" list. */
    private String approvedOn;
}

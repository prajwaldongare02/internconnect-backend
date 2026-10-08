package com.internconnect.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "college_internship_approvals", uniqueConstraints = @UniqueConstraint(columnNames = {"internship_id", "college"}))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CollegeInternshipApproval {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "internship_id", nullable = false) private Internship internship;
    @Column(nullable = false) private String college;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ApprovalStatus status;
    private LocalDateTime decidedAt;
    public enum ApprovalStatus { APPROVED, REJECTED }
}

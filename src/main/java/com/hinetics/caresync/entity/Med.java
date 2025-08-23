package com.hinetics.caresync.entity;

import com.hinetics.caresync.enums.IntakeInstruction;
import com.hinetics.caresync.enums.MedForm;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "meds")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Med {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    private MedForm medForm;

    private String healthCondition;

    private Duration intakeInterval;

    private LocalDateTime startDate;

    private LocalDate endDate;

    private String dosage;

    private  Integer stock;

    private  Integer reminderLimit;

    @Enumerated(EnumType.STRING)
    private IntakeInstruction instruction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

}

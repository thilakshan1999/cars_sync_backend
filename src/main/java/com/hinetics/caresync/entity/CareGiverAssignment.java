package com.hinetics.caresync.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.hinetics.caresync.enums.CareGiverPermission;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareGiverAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "caregiver_id")
    @JsonBackReference
    private User caregiver;

    @ManyToOne
    @JoinColumn(name = "patient_id")
    @JsonBackReference
    private User patient;

    @Enumerated(EnumType.STRING)
    private CareGiverPermission permission = CareGiverPermission.VIEW_ONLY;
}

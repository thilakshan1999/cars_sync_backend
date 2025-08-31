package com.hinetics.caresync.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.hinetics.caresync.enums.CareGiverPermission;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareGiverRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "from_user_id")
    @JsonBackReference
    private User fromUser; // sender (usually caregiver)

    @ManyToOne
    @JoinColumn(name = "to_user_id")
    @JsonBackReference
    private User toUser; // receiver (usually patient)

    @Enumerated(EnumType.STRING)
    private CareGiverPermission requestedPermission;

    private LocalDateTime requestedAt = LocalDateTime.now();
}

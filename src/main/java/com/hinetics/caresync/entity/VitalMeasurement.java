package com.hinetics.caresync.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "vitalMeasurements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VitalMeasurement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime dateTime;

    private String value;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vital_id")
    private Vital vital;
}

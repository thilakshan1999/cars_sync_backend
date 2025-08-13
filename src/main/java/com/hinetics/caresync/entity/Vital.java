package com.hinetics.caresync.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "vitals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vital {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String name;

    // How often to remind (e.g., PT6H for 6 hours)
    private Duration remindDuration;

    private LocalDateTime startDateTime;

    private String unit;

    // One vital has many measurements (dateTime + value)
    @OneToMany(mappedBy = "vital", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VitalMeasurement> measurements;
}

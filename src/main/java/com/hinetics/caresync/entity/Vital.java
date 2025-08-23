package com.hinetics.caresync.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // One vital has many measurements (dateTime + value)
    @OneToMany(mappedBy = "vital", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VitalMeasurement> measurements = new ArrayList<>();

    public void addMeasurement(VitalMeasurement measurement) {
        measurements.add(measurement);
        measurement.setVital(this); // keep both sides in sync
    }

    public void removeMeasurement(VitalMeasurement measurement) {
        measurements.remove(measurement);
        measurement.setVital(null);
    }

    public void setMeasurements(List<VitalMeasurement> newMeasurements) {
        // clear old ones properly
        this.measurements.clear();
        if (newMeasurements != null) {
            newMeasurements.forEach(this::addMeasurement); // ensures back-reference
        }
    }
}

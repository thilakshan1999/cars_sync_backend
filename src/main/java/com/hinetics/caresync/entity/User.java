package com.hinetics.caresync.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.hinetics.caresync.enums.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;

    private LocalDate dateOfBirth;

    private String refreshToken;

    // 👇 Add these fields
    private String otpHash;
    private LocalDateTime otpExpiry;
    private Integer  otpAttempts;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true,fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Document> documents = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Appointment> appointments = new ArrayList<>();

    // 🔹 Medicines
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Med> medicines = new ArrayList<>();

    // 🔹 Vitals
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Vital> vitals = new ArrayList<>();

    // 🔹 Doctors
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Doctor> doctors = new ArrayList<>();

    // 🔹 Caregiver relationships (if role = CAREGIVER)
    @OneToMany(mappedBy = "caregiver", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<CareGiverAssignment> assignedPatients = new ArrayList<>();

    // 🔹 Caregiver relationships (if role = PATIENT)
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<CareGiverAssignment> assignedCaregivers = new ArrayList<>();

    // 🔹 Pending requests sent (for caregivers)
    @OneToMany(mappedBy = "fromUser", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<CareGiverRequest> requestsSent  = new ArrayList<>();

    // 🔹 Pending requests received (for patients)
    @OneToMany(mappedBy = "toUser", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<CareGiverRequest> requestsReceived  = new ArrayList<>();
}

package com.hinetics.caresync.entity;

import com.hinetics.caresync.enums.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
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

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true,fetch = FetchType.LAZY)
    private List<Document> documents = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Appointment> appointments = new ArrayList<>();

    // 🔹 Medicines
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Med> medicines = new ArrayList<>();

    // 🔹 Vitals
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Vital> vitals = new ArrayList<>();

    // 🔹 Doctors
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Doctor> doctors = new ArrayList<>();

    public void addDocument(Document document) {
        documents.add(document);
        document.setUser(this);
    }

    public void addAppointment(Appointment appointment) {
        appointments.add(appointment);
        appointment.setUser(this);
    }

    public void addMedicine(Med medicine) {
        medicines.add(medicine);
        medicine.setUser(this);
    }

    public void addVital(Vital vital) {
        vitals.add(vital);
        vital.setUser(this);
    }

    public void addDoctor(Doctor doctor) {
        doctors.add(doctor);
        doctor.setUser(this);
    }
}

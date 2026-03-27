package com.hinetics.caresync.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "upload_task")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private String fileUrl;
    private String mimeType;

    private Long patientId;
    private String createdBy;

    private String status; // PROCESSING / COMPLETED / FAILED

    @Column(length = 2000)
    private String errorMessage;

}

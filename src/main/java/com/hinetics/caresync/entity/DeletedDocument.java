package com.hinetics.caresync.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "deleted_documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeletedDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long documentId;

    private Long userId;

    private LocalDateTime deletedAt;
}

package com.hinetics.caresync.dto;

import com.hinetics.caresync.enums.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentSummaryDto {
    private Long id;
    private String documentName;
    private DocumentType documentType;
    private String summary;
    private LocalDateTime updatedTime;
}

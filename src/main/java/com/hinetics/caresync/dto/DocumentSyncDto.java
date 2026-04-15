package com.hinetics.caresync.dto;

import com.hinetics.caresync.entity.Document;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentSyncDto {
    private List<DocumentDto> updated;
    private List<Long> deleted;
    private LocalDateTime serverTime;
}

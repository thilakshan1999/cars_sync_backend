package com.hinetics.caresync.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentReferenceDto {
    private Long id;
    private String fileName;
    private String fileType;
    private String signedUrl;
}

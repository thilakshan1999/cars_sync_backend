package com.hinetics.caresync.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResult {
    private  String fileName; // actual stored name in bucket
    private  String fileUrl;
    private  String fileType;
}

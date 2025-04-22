package com.DbBackup.model;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BackupResult {
    private String jobId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean success;
    private String filePath;
    private long fileSize;
    private String errorMessage;
    
    public long getDurationInSeconds() {
        if (startTime == null || endTime == null) {
            return 0;
        }
        return java.time.Duration.between(startTime, endTime).getSeconds();
    }
}

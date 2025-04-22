package com.DbBackup.model;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BackupJob {
     private String id;
    private DatabaseType databaseType;
    private ConnectionParams connectionParams;
    private BackupType backupType;
    private String backupPath;
    private boolean compress;
    private String cronExpression;
    private boolean sendNotification;
    private String slackWebhookUrl;
    private LocalDateTime lastBackupTime;
}

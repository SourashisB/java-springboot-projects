package com.DbBackup.service;

import com.DbBackup.model.BackupJob;
import com.DbBackup.model.BackupResult;

public interface BackupService {
    BackupResult performBackup(BackupJob job);
    boolean scheduleBackup(BackupJob job);
    boolean unscheduleBackup(String jobId);
}

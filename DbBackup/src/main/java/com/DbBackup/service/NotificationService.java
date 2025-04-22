package com.DbBackup.service;

import com.DbBackup.model.BackupResult;

public interface NotificationService {
    void sendSlackNotification(BackupResult result, String webhookUrl);
}

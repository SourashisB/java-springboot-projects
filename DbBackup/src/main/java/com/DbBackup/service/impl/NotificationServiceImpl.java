package com.DbBackup.service.impl;

import org.springframework.stereotype.Service;

import com.DbBackup.service.NotificationService;
import com.DbBackup.model.BackupResult;
import com.slack.api.Slack;
import com.slack.api.webhook.Payload;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final Slack slack = Slack.getInstance();

    @Override
    public void sendSlackNotification(BackupResult result, String webhookUrl) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.warn("Slack webhook URL is not provided, skipping notification");
            return;
        }

        try {
            Payload payload = buildSlackPayload(result);
            slack.send(webhookUrl, payload);
            log.info("Slack notification sent successfully for job {}", result.getJobId());
        } catch (IOException e) {
            log.error("Failed to send Slack notification: {}", e.getMessage());
        }
    }

    private Payload buildSlackPayload(BackupResult result) {
        Map<String, Object> attachment = new HashMap<>();
        List<Map<String, Object>> attachments = new ArrayList<>();

        String color = result.isSuccess() ? "#36a64f" : "#ff0000";
        String title = result.isSuccess()
                ? "Database backup completed successfully"
                : "Database backup failed";

        attachment.put("color", color);
        attachment.put("title", title);

        List<Map<String, Object>> fields = new ArrayList<>();

        Map<String, Object> jobIdField = new HashMap<>();
        jobIdField.put("title", "Job ID");
        jobIdField.put("value", result.getJobId());
        jobIdField.put("short", true);
        fields.add(jobIdField);

        if (result.getStartTime() != null) {
            Map<String, Object> startTimeField = new HashMap<>();
            startTimeField.put("title", "Start Time");
            startTimeField.put("value", result.getStartTime().format(DATE_FORMATTER));
            startTimeField.put("short", true);
            fields.add(startTimeField);
        }

        if (result.getEndTime() != null) {
            Map<String, Object> endTimeField = new HashMap<>();
            endTimeField.put("title", "End Time");
            endTimeField.put("value", result.getEndTime().format(DATE_FORMATTER));
            endTimeField.put("short", true);
            fields.add(endTimeField);
        }

        Map<String, Object> durationField = new HashMap<>();
        durationField.put("title", "Duration");
        durationField.put("value", result.getDurationInSeconds() + " seconds");
        durationField.put("short", true);
        fields.add(durationField);

        if (result.isSuccess() && result.getFilePath() != null) {
            Map<String, Object> filePathField = new HashMap<>();
            filePathField.put("title", "Backup File");
            filePathField.put("value", result.getFilePath());
            filePathField.put("short", false);
            fields.add(filePathField);

            Map<String, Object> fileSizeField = new HashMap<>();
            fileSizeField.put("title", "File Size");
            fileSizeField.put("value", formatFileSize(result.getFileSize()));
            fileSizeField.put("short", true);
            fields.add(fileSizeField);
        }

        if (!result.isSuccess() && result.getErrorMessage() != null) {
            Map<String, Object> errorField = new HashMap<>();
            errorField.put("title", "Error");
            errorField.put("value", result.getErrorMessage());
            errorField.put("short", false);
            fields.add(errorField);
        }

        attachment.put("fields", fields);
        attachments.add(attachment);

        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("attachments", attachments);

        return Payload.builder().blocks(new ArrayList<>()).attachments(attachments).build();
    }

    private String formatFileSize(long size) {
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int unitIndex = 0;
        double fileSize = size;

        while (fileSize > 1024 && unitIndex < units.length - 1) {
            fileSize /= 1024;
            unitIndex++;
        }

        return String.format("%.2f %s", fileSize, units[unitIndex]);
    }
}

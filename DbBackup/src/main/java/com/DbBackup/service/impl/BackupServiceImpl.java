package com.DbBackup.service.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerContext;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.stereotype.Service;

import com.DbBackup.model.BackupJob;
import com.DbBackup.model.BackupResult;
import com.DbBackup.model.DatabaseType;
import com.DbBackup.service.BackupService;
import com.DbBackup.service.CompressionService;
import com.DbBackup.service.DatabaseConnectionService;
import com.DbBackup.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import java.io.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class BackupServiceImpl implements BackupService {
    
    private final DatabaseConnectionService connectionService;
    private final CompressionService compressionService;
    private final NotificationService notificationService;
    private final Scheduler scheduler;

    @Override
    public BackupResult performBackup(BackupJob job) {
        LocalDateTime startTime = LocalDateTime.now();
        log.info("Starting backup job {} at {}", job.getId(), startTime);
        
        BackupResult.BackupResultBuilder resultBuilder = BackupResult.builder()
                .jobId(job.getId())
                .startTime(startTime)
                .success(false);
        
        // Test database connection first
        if (!connectionService.testConnection(job.getDatabaseType(), job.getConnectionParams())) {
            String errorMsg = "Failed to connect to database";
            log.error(errorMsg);
            return resultBuilder
                    .endTime(LocalDateTime.now())
                    .errorMessage(errorMsg)
                    .build();
        }
        
        // Create backup directory if it doesn't exist
        Path backupDir = Paths.get(job.getBackupPath());
        try {
            if (!Files.exists(backupDir)) {
                Files.createDirectories(backupDir);
            }
        } catch (IOException e) {
            String errorMsg = "Failed to create backup directory: " + e.getMessage();
            log.error(errorMsg);
            return resultBuilder
                    .endTime(LocalDateTime.now())
                    .errorMessage(errorMsg)
                    .build();
        }
        
        // Generate backup file name
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(startTime);
        String backupFileName = String.format("%s_%s_%s_backup.%s", 
                job.getDatabaseType().toString().toLowerCase(),
                job.getConnectionParams().getDatabase(),
                timestamp,
                getFileExtension(job.getDatabaseType()));
        
        String backupFilePath = Paths.get(job.getBackupPath(), backupFileName).toString();
        
        try {
            // Perform database-specific backup
            switch (job.getDatabaseType()) {
                case MYSQL:
                    backupMySql(job, backupFilePath);
                    break;
                case POSTGRESQL:
                    backupPostgres(job, backupFilePath);
                    break;
                case MONGODB:
                    backupMongoDB(job, backupFilePath);
                    break;
                case SQLITE:
                    backupSqlite(job, backupFilePath);
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported database type: " + job.getDatabaseType());
            }
            
            // Compress the backup file if requested
            String finalFilePath = backupFilePath;
            if (job.isCompress()) {
                finalFilePath = compressionService.compressFile(backupFilePath);
                // Delete the original uncompressed file
                Files.deleteIfExists(Paths.get(backupFilePath));
            }
            
            // Get file size
            long fileSize = Files.size(Paths.get(finalFilePath));
            
            LocalDateTime endTime = LocalDateTime.now();
            
            // Build successful result
            BackupResult result = resultBuilder
                    .success(true)
                    .filePath(finalFilePath)
                    .fileSize(fileSize)
                    .endTime(endTime)
                    .build();
            
            log.info("Backup completed successfully: {} ({})", finalFilePath, formatFileSize(fileSize));
            
            // Send notification if enabled
            if (job.isSendNotification() && job.getSlackWebhookUrl() != null) {
                notificationService.sendSlackNotification(result, job.getSlackWebhookUrl());
            }
            
            // Update job's last backup time
            job.setLastBackupTime(endTime);
            
            return result;
            
        } catch (Exception e) {
            String errorMsg = "Backup failed: " + e.getMessage();
            log.error(errorMsg, e);
            
            BackupResult result = resultBuilder
                    .endTime(LocalDateTime.now())
                    .errorMessage(errorMsg)
                    .build();
            
            // Send notification about failure if enabled
            if (job.isSendNotification() && job.getSlackWebhookUrl() != null) {
                notificationService.sendSlackNotification(result, job.getSlackWebhookUrl());
            }
            
            return result;
        }
    }

    @Override
    public boolean scheduleBackup(BackupJob job) {
        try {
            if (job.getId() == null || job.getId().isEmpty()) {
                job.setId(UUID.randomUUID().toString());
            }
            
            JobDetail jobDetail = JobBuilder.newJob(BackupJobExecutor.class)
                    .withIdentity("backup-" + job.getId())
                    .usingJobData("jobId", job.getId())
                    .storeDurably()
                    .build();
            
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("trigger-" + job.getId())
                    .withSchedule(CronScheduleBuilder.cronSchedule(job.getCronExpression()))
                    .build();
            
            // Store job data in scheduler context
            scheduler.getContext().put("job-" + job.getId(), job);
            
            // Schedule the job
            scheduler.scheduleJob(jobDetail, trigger);
            log.info("Scheduled backup job {} with cron expression: {}", job.getId(), job.getCronExpression());
            
            return true;
        } catch (Exception e) {
            log.error("Failed to schedule backup job: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean unscheduleBackup(String jobId) {
        try {
            boolean result = scheduler.unscheduleJob(TriggerKey.triggerKey("trigger-" + jobId));
            if (result) {
                scheduler.deleteJob(JobKey.jobKey("backup-" + jobId));
                scheduler.getContext().remove("job-" + jobId);
                log.info("Unscheduled backup job: {}", jobId);
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to unschedule backup job: {}", e.getMessage());
            return false;
        }
    }
    
    // Quartz Job class for executing backups
    public static class BackupJobExecutor implements Job {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            try {
                String jobId = context.getJobDetail().getJobDataMap().getString("jobId");
                SchedulerContext schedulerContext = context.getScheduler().getContext();
                BackupJob job = (BackupJob) schedulerContext.get("job-" + jobId);
                
                if (job != null) {
                    BackupService backupService = (BackupService) schedulerContext.get("backupService");
                    backupService.performBackup(job);
                } else {
                    throw new JobExecutionException("Backup job not found: " + jobId);
                }
            } catch (Exception e) {
                throw new JobExecutionException("Error executing backup job: " + e.getMessage(), e);
            }
        }
    }
    
    private void backupMySql(BackupJob job, String outputFile) throws IOException, InterruptedException {
        String host = job.getConnectionParams().getHost();
        int port = job.getConnectionParams().getPort() != null ? job.getConnectionParams().getPort() : 3306;
        String username = job.getConnectionParams().getUsername();
        String password = job.getConnectionParams().getPassword();
        String database = job.getConnectionParams().getDatabase();
        
        ProcessBuilder pb = new ProcessBuilder(
                "mysqldump",
                "--host=" + host,
                "--port=" + port,
                "--user=" + username,
                "--password=" + password,
                "--single-transaction",
                "--routines",
                "--triggers",
                "--events",
                database
        );
        
        executeCommand(pb, outputFile);
    }
    
    private void backupPostgres(BackupJob job, String outputFile) throws IOException, InterruptedException {
        String host = job.getConnectionParams().getHost();
        int port = job.getConnectionParams().getPort() != null ? job.getConnectionParams().getPort() : 5432;
        String username = job.getConnectionParams().getUsername();
        String database = job.getConnectionParams().getDatabase();
        
        // Set PGPASSWORD environment variable for password
        ProcessBuilder pb = new ProcessBuilder(
                "pg_dump",
                "-h", host,
                "-p", String.valueOf(port),
                "-U", username,
                "-F", "c", // Custom format
                "-b", // Include large objects
                "-v", // Verbose
                "-f", outputFile,
                database
        );
        
        pb.environment().put("PGPASSWORD", job.getConnectionParams().getPassword());
        
        executeCommand(pb, null);
    }
    
    private void backupMongoDB(BackupJob job, String outputFile) throws IOException, InterruptedException {
        String host = job.getConnectionParams().getHost();
        int port = job.getConnectionParams().getPort() != null ? job.getConnectionParams().getPort() : 27017;
        String username = job.getConnectionParams().getUsername();
        String password = job.getConnectionParams().getPassword();
        String database = job.getConnectionParams().getDatabase();
        
        // Create directory for the backup
        Path outputDir = Paths.get(outputFile);
        Files.createDirectories(outputDir);
        
        ProcessBuilder pb;
        if (username != null && !username.isEmpty()) {
            pb = new ProcessBuilder(
                    "mongodump",
                    "--host", host,
                    "--port", String.valueOf(port),
                    "--username", username,
                    "--password", password,
                    "--db", database,
                    "--out", outputDir.toString()
            );
        } else {
            pb = new ProcessBuilder(
                    "mongodump",
                    "--host", host,
                    "--port", String.valueOf(port),
                    "--db", database,
                    "--out", outputDir.toString()
            );
        }
        
        executeCommand(pb, null);
    }
    
    private void backupSqlite(BackupJob job, String outputFile) throws IOException, InterruptedException {
        String database = job.getConnectionParams().getDatabase();
        
        // For SQLite, we just copy the database file
        Files.copy(Paths.get(database), Paths.get(outputFile));
    }
    
    private void executeCommand(ProcessBuilder pb, String outputFile) throws IOException, InterruptedException {
        pb.redirectErrorStream(true);
        
        Process process;
        if (outputFile != null) {
            pb.redirectOutput(ProcessBuilder.Redirect.to(new File(outputFile)));
            process = pb.start();
        } else {
            process = pb.start();
        }
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                throw new IOException("Command exited with code " + exitCode + ": " + output);
            }
        }
    }
    
    private String getFileExtension(DatabaseType type) {
        switch (type) {
            case MYSQL:
                return "sql";
            case POSTGRESQL:
                return "dump";
            case MONGODB:
                return "dir";
            case SQLITE:
                return "db";
            default:
                return "bak";
        }
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

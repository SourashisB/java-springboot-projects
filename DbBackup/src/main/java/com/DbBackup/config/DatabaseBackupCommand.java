package com.DbBackup.config;

import com.DbBackup.model.BackupJob;
import com.DbBackup.model.BackupResult;
import com.DbBackup.model.BackupType;
import com.DbBackup.model.ConnectionParams;
import com.DbBackup.model.DatabaseType;
import com.DbBackup.service.BackupService;
import com.DbBackup.service.DatabaseConnectionService;
import com.DbBackup.service.RestoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

@Component
@Command(
    name = "db-backup",
    description = "Database Backup CLI Tool",
    mixinStandardHelpOptions = true,
    subcommands = {
        DatabaseBackupCommand.BackupCommand.class,
        DatabaseBackupCommand.RestoreCommand.class,
        DatabaseBackupCommand.ScheduleCommand.class,
        DatabaseBackupCommand.UnscheduleCommand.class,
        DatabaseBackupCommand.TestConnectionCommand.class
    }
)
public class DatabaseBackupCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        // This method will be called if no subcommand is specified
        System.out.println("Please specify a subcommand. Use --help for more information.");
        return 0;
    }

    @Component
    @Command(
        name = "backup",
        description = "Perform a database backup",
        mixinStandardHelpOptions = true
    )
    @RequiredArgsConstructor
    public static class BackupCommand implements Callable<Integer> {
        
        private final BackupService backupService;
        
        @Option(names = {"-t", "--type"}, description = "Database type: MYSQL, POSTGRESQL, MONGODB, SQLITE", required = true)
        private DatabaseType databaseType;
        
        @Option(names = {"-h", "--host"}, description = "Database host", defaultValue = "localhost")
        private String host;
        
        @Option(names = {"-P", "--port"}, description = "Database port")
        private Integer port;
        
        @Option(names = {"-u", "--user"}, description = "Database username")
        private String username;
        
        @Option(names = {"-p", "--password"}, description = "Database password", interactive = true)
        private String password;
        
        @Option(names = {"-d", "--database"}, description = "Database name", required = true)
        private String database;
        
        @Option(names = {"-o", "--output"}, description = "Output directory for backup files", defaultValue = "./backups")
        private String outputDir;
        
        @Option(names = {"-b", "--backup-type"}, description = "Backup type: FULL, INCREMENTAL, DIFFERENTIAL", defaultValue = "FULL")
        private BackupType backupType;
        
        @Option(names = {"-c", "--compress"}, description = "Compress backup file", defaultValue = "true")
        private boolean compress;
        
        @Option(names = {"-n", "--notify"}, description = "Send notification on completion")
        private boolean notify;
        
        @Option(names = {"--slack-webhook"}, description = "Slack webhook URL for notifications")
        private String slackWebhookUrl;
        
        @Override
        public Integer call() {
            try {
                ConnectionParams connectionParams = ConnectionParams.builder()
                        .host(host)
                        .port(port)
                        .username(username)
                        .password(password)
                        .database(database)
                        .build();
                
                BackupJob job = BackupJob.builder()
                        .id(UUID.randomUUID().toString())
                        .databaseType(databaseType)
                        .connectionParams(connectionParams)
                        .backupType(backupType)
                        .backupPath(outputDir)
                        .compress(compress)
                        .sendNotification(notify)
                        .slackWebhookUrl(slackWebhookUrl)
                        .build();
                
                System.out.println("Starting backup job...");
                BackupResult result = backupService.performBackup(job);
                
                if (result.isSuccess()) {
                    System.out.println("Backup completed successfully!");
                    System.out.println("Backup file: " + result.getFilePath());
                    System.out.println("Start time: " + formatDateTime(result.getStartTime()));
                    System.out.println("End time: " + formatDateTime(result.getEndTime()));
                    System.out.println("Duration: " + result.getDurationInSeconds() + " seconds");
                    return 0;
                } else {
                    System.out.println("Backup failed: " + result.getErrorMessage());
                    return 1;
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                return 1;
            }
        }
        
        private String formatDateTime(LocalDateTime dateTime) {
            return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
    }
    
    @Component
    @Command(
        name = "restore",
        description = "Restore a database from backup",
        mixinStandardHelpOptions = true
    )
    @RequiredArgsConstructor
    public static class RestoreCommand implements Callable<Integer> {
        
        private final RestoreService restoreService;
        
        @Option(names = {"-t", "--type"}, description = "Database type: MYSQL, POSTGRESQL, MONGODB, SQLITE", required = true)
        private DatabaseType databaseType;
        
        @Option(names = {"-h", "--host"}, description = "Database host", defaultValue = "localhost")
        private String host;
        
        @Option(names = {"-P", "--port"}, description = "Database port")
        private Integer port;
        
        @Option(names = {"-u", "--user"}, description = "Database username")
        private String username;
        
        @Option(names = {"-p", "--password"}, description = "Database password", interactive = true)
        private String password;
        
        @Option(names = {"-d", "--database"}, description = "Database name", required = true)
        private String database;
        
        @Option(names = {"--items"}, description = "Specific tables/collections to restore (comma-separated)")
        private String items;
        
        @Parameters(index = "0", description = "Backup file or directory path", paramLabel = "BACKUP_PATH")
        private String backupPath;
        
        @Override
        public Integer call() {
            try {
                ConnectionParams connectionParams = ConnectionParams.builder()
                        .host(host)
                        .port(port)
                        .username(username)
                        .password(password)
                        .database(database)
                        .build();
                
                List<String> itemsList = null;
                if (items != null && !items.isBlank()) {
                    itemsList = Arrays.asList(items.split(","));
                }
                
                System.out.println("Starting restore operation...");
                boolean success;
                
                if (itemsList != null && !itemsList.isEmpty()) {
                    System.out.println("Performing selective restore for: " + String.join(", ", itemsList));
                    success = restoreService.restoreSelectiveBackup(backupPath, databaseType, connectionParams, itemsList);
                } else {
                    System.out.println("Performing full restore");
                    success = restoreService.restoreBackup(backupPath, databaseType, connectionParams);
                }
                
                if (success) {
                    System.out.println("Restore completed successfully!");
                    return 0;
                } else {
                    System.out.println("Restore failed");
                    return 1;
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                return 1;
            }
        }
    }
    
    @Component
    @Command(
        name = "schedule",
        description = "Schedule a recurring backup job",
        mixinStandardHelpOptions = true
    )
    @RequiredArgsConstructor
    public static class ScheduleCommand implements Callable<Integer> {
        
        private final BackupService backupService;
        
        @Option(names = {"-t", "--type"}, description = "Database type: MYSQL, POSTGRESQL, MONGODB, SQLITE", required = true)
        private DatabaseType databaseType;
        
        @Option(names = {"-h", "--host"}, description = "Database host", defaultValue = "localhost")
        private String host;
        
        @Option(names = {"-P", "--port"}, description = "Database port")
        private Integer port;
        
        @Option(names = {"-u", "--user"}, description = "Database username")
        private String username;
        
        @Option(names = {"-p", "--password"}, description = "Database password", interactive = true)
        private String password;
        
        @Option(names = {"-d", "--database"}, description = "Database name", required = true)
        private String database;
        
        @Option(names = {"-o", "--output"}, description = "Output directory for backup files", defaultValue = "./backups")
        private String outputDir;
        
        @Option(names = {"-b", "--backup-type"}, description = "Backup type: FULL, INCREMENTAL, DIFFERENTIAL", defaultValue = "FULL")
        private BackupType backupType;
        
        @Option(names = {"-c", "--compress"}, description = "Compress backup file", defaultValue = "true")
        private boolean compress;
        
        @Option(names = {"-n", "--notify"}, description = "Send notification on completion")
        private boolean notify;
        
        @Option(names = {"--slack-webhook"}, description = "Slack webhook URL for notifications")
        private String slackWebhookUrl;
        
        @Option(names = {"--cron"}, description = "Cron expression for scheduling (e.g., '0 0 * * * ?' for daily at midnight)", required = true)
        private String cronExpression;
        
        @Option(names = {"--id"}, description = "Custom job ID (optional)")
        private String jobId;
        
        @Override
        public Integer call() {
            try {
                ConnectionParams connectionParams = ConnectionParams.builder()
                        .host(host)
                        .port(port)
                        .username(username)
                        .password(password)
                        .database(database)
                        .build();
                
                BackupJob job = BackupJob.builder()
                        .id(jobId != null ? jobId : UUID.randomUUID().toString())
                        .databaseType(databaseType)
                        .connectionParams(connectionParams)
                        .backupType(backupType)
                        .backupPath(outputDir)
                        .compress(compress)
                        .cronExpression(cronExpression)
                        .sendNotification(notify)
                        .slackWebhookUrl(slackWebhookUrl)
                        .build();
                
                boolean scheduled = backupService.scheduleBackup(job);
                
                if (scheduled) {
                    System.out.println("Backup job scheduled successfully!");
                    System.out.println("Job ID: " + job.getId());
                    System.out.println("Cron expression: " + cronExpression);
                    System.out.println("Output directory: " + Paths.get(outputDir).toAbsolutePath());
                    return 0;
                } else {
                    System.out.println("Failed to schedule backup job");
                    return 1;
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                return 1;
            }
        }
    }
    
    @Component
    @Command(
        name = "unschedule",
        description = "Unschedule a recurring backup job",
        mixinStandardHelpOptions = true
    )
    @RequiredArgsConstructor
    public static class UnscheduleCommand implements Callable<Integer> {
        
        private final BackupService backupService;
        
        @Parameters(index = "0", description = "Job ID", paramLabel = "JOB_ID")
        private String jobId;
        
        @Override
        public Integer call() {
            try {
                boolean unscheduled = backupService.unscheduleBackup(jobId);
                
                if (unscheduled) {
                    System.out.println("Backup job unscheduled successfully!");
                    return 0;
                } else {
                    System.out.println("Failed to unschedule backup job, job ID may not exist");
                    return 1;
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                return 1;
            }
        }
    }
    
    @Component
    @Command(
        name = "test-connection",
        description = "Test database connection",
        mixinStandardHelpOptions = true
    )
    @RequiredArgsConstructor
    public static class TestConnectionCommand implements Callable<Integer> {
        
        private final DatabaseConnectionService connectionService;
        
        @Option(names = {"-t", "--type"}, description = "Database type: MYSQL, POSTGRESQL, MONGODB, SQLITE", required = true)
        private DatabaseType databaseType;
        
        @Option(names = {"-h", "--host"}, description = "Database host", defaultValue = "localhost")
        private String host;
        
        @Option(names = {"-P", "--port"}, description = "Database port")
        private Integer port;
        
        @Option(names = {"-u", "--user"}, description = "Database username")
        private String username;
        
        @Option(names = {"-p", "--password"}, description = "Database password", interactive = true)
        private String password;
        
        @Option(names = {"-d", "--database"}, description = "Database name", required = true)
        private String database;
        
        @Override
        public Integer call() {
            try {
                ConnectionParams connectionParams = ConnectionParams.builder()
                        .host(host)
                        .port(port)
                        .username(username)
                        .password(password)
                        .database(database)
                        .build();
                
                System.out.println("Testing connection to " + databaseType + " database...");
                boolean success = connectionService.testConnection(databaseType, connectionParams);
                
                if (success) {
                    System.out.println("Connection successful!");
                    return 0;
                } else {
                    System.out.println("Connection failed");
                    return 1;
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                return 1;
            }
        }
    }
}
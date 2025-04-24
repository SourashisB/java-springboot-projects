package com.DbBackup.config;
import com.DbBackup.service.BackupService;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CliConfig {

    @Bean
    @Autowired
    public void registerBackupServiceInScheduler(Scheduler scheduler, BackupService backupService) throws Exception {
        scheduler.getContext().put("backupService", backupService);
    }
}
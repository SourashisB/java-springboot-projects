package com.DbBackup.service.impl;

import com.DbBackup.model.ConnectionParams;
import com.DbBackup.model.DatabaseType;
import com.DbBackup.service.CompressionService; 
import com.DbBackup.service.DatabaseConnectionService;
import com.DbBackup.service.RestoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RestoreServiceImpl implements RestoreService {

    private final DatabaseConnectionService connectionService;
    private final CompressionService compressionService;
    
    @Override
    public boolean restoreBackup(String backupFilePath, DatabaseType type, ConnectionParams connectionParams) {
        return restoreSelectiveBackup(backupFilePath, type, connectionParams, null);
    }

    @Override
    public boolean restoreSelectiveBackup(String backupFilePath, DatabaseType type, 
                                         ConnectionParams connectionParams, List<String> items) {
        log.info("Starting restore from {} for database type {}", backupFilePath, type);
        
        // Test connection first
        if (!connectionService.testConnection(type, connectionParams)) {
            log.error("Failed to connect to database");
            return false;
        }
        
        try {
            // Decompress if it's a compressed file
            String filePath = backupFilePath;
            if (backupFilePath.endsWith(".tar.gz")) {
                filePath = compressionService.decompressFile(backupFilePath);
            }
            
            // Perform database-specific restore
            switch (type) {
                case MYSQL:
                    return restoreMySql(filePath, connectionParams, items);
                case POSTGRESQL:
                    return restorePostgres(filePath, connectionParams, items);
                case MONGODB:
                    return restoreMongoDB(filePath, connectionParams, items);
                case SQLITE:
                    return restoreSqlite(filePath, connectionParams);
                default:
                    log.error("Unsupported database type: {}", type);
                    return false;
            }
        } catch (Exception e) {
            log.error("Restore failed: {}", e.getMessage(), e);
            return false;
        }
    }
    
    private boolean restoreMySql(String backupFile, ConnectionParams params, List<String> tables) 
            throws IOException, InterruptedException {
        String host = params.getHost();
        int port = params.getPort() != null ? params.getPort() : 3306;
        String username = params.getUsername();
        String password = params.getPassword();
        String database = params.getDatabase();
        
        ProcessBuilder pb;
        
        if (tables != null && !tables.isEmpty()) {
            // For selective restore, we need to extract only specific tables
            // This is a simplified approach - actual implementation would need to parse the SQL file
            log.warn("Selective restore for MySQL is not fully implemented, will restore all tables");
        }
        
        pb = new ProcessBuilder(
                "mysql",
                "--host=" + host,
                "--port=" + port,
                "--user=" + username,
                "--password=" + password,
                database
        );
        
        pb.redirectInput(new File(backupFile));
        return executeCommand(pb);
    }
    
    private boolean restorePostgres(String backupFile, ConnectionParams params, List<String> tables) 
            throws IOException, InterruptedException {
        String host = params.getHost();
        int port = params.getPort() != null ? params.getPort() : 5432;
        String username = params.getUsername();
        String database = params.getDatabase();
        
        ProcessBuilder pb;
        
        if (tables != null && !tables.isEmpty()) {
            // For selective restore with pg_restore
            String[] tableArgs = new String[tables.size() * 2];
            for (int i = 0; i < tables.size(); i++) {
                tableArgs[i * 2] = "-t";
                tableArgs[i * 2 + 1] = tables.get(i);
            }
            
            String[] baseCommand = {
                    "pg_restore",
                    "-h", host,
                    "-p", String.valueOf(port),
                    "-U", username,
                    "-d", database,
                    "-v" // Verbose
            };
            
            String[] fullCommand = new String[baseCommand.length + tableArgs.length + 1];
            System.arraycopy(baseCommand, 0, fullCommand, 0, baseCommand.length);
            System.arraycopy(tableArgs, 0, fullCommand, baseCommand.length, tableArgs.length);
            fullCommand[fullCommand.length - 1] = backupFile;
            
            pb = new ProcessBuilder(fullCommand);
        } else {
            // Full restore
            pb = new ProcessBuilder(
                    "pg_restore",
                    "-h", host,
                    "-p", String.valueOf(port),
                    "-U", username,
                    "-d", database,
                    "-v", // Verbose
                    backupFile
            );
        }
        
        pb.environment().put("PGPASSWORD", params.getPassword());
        return executeCommand(pb);
    }
    
    private boolean restoreMongoDB(String backupDir, ConnectionParams params, List<String> collections) 
            throws IOException, InterruptedException {
        String host = params.getHost();
        int port = params.getPort() != null ? params.getPort() : 27017;
        String username = params.getUsername();
        String password = params.getPassword();
        String database = params.getDatabase();
        
        // Check if the path is a directory (from mongodump)
        Path path = Paths.get(backupDir);
        if (!Files.isDirectory(path)) {
            log.error("MongoDB backup must be a directory: {}", backupDir);
            return false;
        }
        
        // Check if database directory exists within the backup
        Path dbPath = path.resolve(database);
        if (!Files.isDirectory(dbPath)) {
            log.error("Database directory not found in backup: {}", dbPath);
            return false;
        }
        
        ProcessBuilder pb;
        
        if (collections != null && !collections.isEmpty()) {
            // Restore specific collections
            boolean atLeastOneSuccess = false;
            
            for (String collection : collections) {
                // Check if collection file exists
                Path collectionFile = dbPath.resolve(collection + ".bson");
                if (!Files.exists(collectionFile)) {
                    log.warn("Collection file not found: {}", collectionFile);
                    continue;
                }
                
                if (username != null && !username.isEmpty()) {
                    pb = new ProcessBuilder(
                            "mongorestore",
                            "--host", host,
                            "--port", String.valueOf(port),
                            "--username", username,
                            "--password", password,
                            "--db", database,
                            "--collection", collection,
                            collectionFile.toString()
                    );
                } else {
                    pb = new ProcessBuilder(
                            "mongorestore",
                            "--host", host,
                            "--port", String.valueOf(port),
                            "--db", database,
                            "--collection", collection,
                            collectionFile.toString()
                    );
                }
                
                boolean success = executeCommand(pb);
                if (success) {
                    atLeastOneSuccess = true;
                }
            }
            
            return atLeastOneSuccess;
        } else {
            // Restore entire database
            if (username != null && !username.isEmpty()) {
                pb = new ProcessBuilder(
                        "mongorestore",
                        "--host", host,
                        "--port", String.valueOf(port),
                        "--username", username,
                        "--password", password,
                        "--db", database,
                        dbPath.toString()
                );
            } else {
                pb = new ProcessBuilder(
                        "mongorestore",
                        "--host", host,
                        "--port", String.valueOf(port),
                        "--db", database,
                        dbPath.toString()
                );
            }
            
            return executeCommand(pb);
        }
    }
    
    private boolean restoreSqlite(String backupFile, ConnectionParams params) throws IOException {
        // For SQLite, we just copy the backup file to the target location
        String database = params.getDatabase();
        Files.copy(Paths.get(backupFile), Paths.get(database));
        return true;
    }
    
    private boolean executeCommand(ProcessBuilder pb) throws IOException, InterruptedException {
        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        // Read and log the output
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            log.error("Command failed with exit code {}: {}", exitCode, output);
            return false;
        } else {
            log.info("Command executed successfully: {}", output);
            return true;
        }
    }
}
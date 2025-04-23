package com.DbBackup.service.impl;

import com.DbBackup.model.ConnectionParams;
import com.DbBackup.service.DatabaseConnectionService;
import com.DbBackup.model.DatabaseType;
import com.mongodb.client.MongoClients;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;

@Service
@Slf4j
public class DatabaseConnectionServiceImpl implements DatabaseConnectionService {

    @Override
    public boolean testConnection(DatabaseType type, ConnectionParams params) {
        try {
            switch (type) {
                case MYSQL:
                    return testMySqlConnection(params);
                case POSTGRESQL:
                    return testPostgresConnection(params);
                case MONGODB:
                    return testMongoConnection(params);
                case SQLITE:
                    return testSqliteConnection(params);
                default:
                    log.error("Unsupported database type: {}", type);
                    return false;
            }
        } catch (Exception e) {
            log.error("Error testing connection to {} database: {}", type, e.getMessage());
            return false;
        }
    }

    private boolean testMySqlConnection(ConnectionParams params) throws Exception {
        String url = String.format("jdbc:mysql://%s:%d/%s", 
                params.getHost(), 
                params.getPort() != null ? params.getPort() : 3306, 
                params.getDatabase());
        
        try (Connection conn = DriverManager.getConnection(url, params.getUsername(), params.getPassword())) {
            return conn.isValid(5);
        }
    }

    private boolean testPostgresConnection(ConnectionParams params) throws Exception {
        String url = String.format("jdbc:postgresql://%s:%d/%s", 
                params.getHost(), 
                params.getPort() != null ? params.getPort() : 5432, 
                params.getDatabase());
        
        try (Connection conn = DriverManager.getConnection(url, params.getUsername(), params.getPassword())) {
            return conn.isValid(5);
        }
    }

    private boolean testMongoConnection(ConnectionParams params) {
        String connectionString;
        if (params.getUsername() != null && !params.getUsername().isEmpty()) {
            connectionString = String.format("mongodb://%s:%s@%s:%d/%s", 
                    params.getUsername(), 
                    params.getPassword(), 
                    params.getHost(), 
                    params.getPort() != null ? params.getPort() : 27017, 
                    params.getDatabase());
        } else {
            connectionString = String.format("mongodb://%s:%d/%s", 
                    params.getHost(), 
                    params.getPort() != null ? params.getPort() : 27017, 
                    params.getDatabase());
        }
        
        try (var client = MongoClients.create(connectionString)) {
            client.getDatabase(params.getDatabase()).runCommand(new org.bson.Document("ping", 1));
            return true;
        } catch (Exception e) {
            log.error("MongoDB connection test failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean testSqliteConnection(ConnectionParams params) throws Exception {
        String url = String.format("jdbc:sqlite:%s", params.getDatabase());
        
        try (Connection conn = DriverManager.getConnection(url)) {
            return conn.isValid(5);
        }
    }
    
}

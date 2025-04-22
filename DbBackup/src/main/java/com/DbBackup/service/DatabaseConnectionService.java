package com.DbBackup.service;

import com.DbBackup.model.ConnectionParams;
import com.DbBackup.model.DatabaseType;

public interface DatabaseConnectionService {
    boolean testConnection(DatabaseType type, ConnectionParams connectionParams);
}
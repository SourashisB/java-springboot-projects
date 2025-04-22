package com.DbBackup.service;

import java.util.List;

import com.DbBackup.model.ConnectionParams;
import com.DbBackup.model.DatabaseType;

public interface RestoreService {

    boolean restoreBackup(String backupFilePath, DatabaseType type, ConnectionParams connectionParams);
    boolean restoreSelectiveBackup(String backupFilePath, DatabaseType type, ConnectionParams connectionParams, List<String> items);
}
package com.DbBackup.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConnectionParams {
    private String host;
    private Integer port;
    private String username;
    private String password;
    private String database;
    private String additionalParams;
}

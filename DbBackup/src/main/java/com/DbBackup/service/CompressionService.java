package com.DbBackup.service;

public interface CompressionService {

    String compressFile(String filePath);
    String decompressFile(String compressedFilePath);
}
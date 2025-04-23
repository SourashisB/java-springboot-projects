package com.DbBackup.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import com.DbBackup.service.CompressionService;

@Service
@Slf4j
public class CompressionServiceImpl implements CompressionService{

    @Override
    public String compressFile(String filePath) {
        String compressedFilePath = filePath + ".tar.gz";
        File sourceFile = new File(filePath);
        
        try (FileOutputStream fos = new FileOutputStream(compressedFilePath);
             GzipCompressorOutputStream gzos = new GzipCompressorOutputStream(fos);
             TarArchiveOutputStream taos = new TarArchiveOutputStream(gzos)) {
            
            taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
            
            TarArchiveEntry entry = new TarArchiveEntry(sourceFile, sourceFile.getName());
            taos.putArchiveEntry(entry);
            
            try (FileInputStream fis = new FileInputStream(sourceFile)) {
                IOUtils.copy(fis, taos);
            }
            
            taos.closeArchiveEntry();
            log.info("Successfully compressed file: {} to {}", filePath, compressedFilePath);
            return compressedFilePath;
            
        } catch (IOException e) {
            log.error("Error compressing file: {}", e.getMessage());
            return filePath; // Return original file path if compression fails
        }
    }

    @Override
    public String decompressFile(String compressedFilePath) {
        if (!compressedFilePath.endsWith(".tar.gz")) {
            log.warn("File is not a tar.gz archive: {}", compressedFilePath);
            return compressedFilePath;
        }
        
        String outputDir = Paths.get(compressedFilePath).getParent().toString();
        
        try (FileInputStream fis = new FileInputStream(compressedFilePath);
             GzipCompressorInputStream gzis = new GzipCompressorInputStream(fis);
             TarArchiveInputStream tais = new TarArchiveInputStream(gzis)) {
            
            TarArchiveEntry entry;
            while ((entry = tais.getNextTarEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                
                File outputFile = new File(outputDir, entry.getName());
                Path parent = Paths.get(outputFile.getParent());
                if (!Files.exists(parent)) {
                    Files.createDirectories(parent);
                }
                
                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    IOUtils.copy(tais, fos);
                }
                
                log.info("Extracted file: {}", outputFile.getAbsolutePath());
                return outputFile.getAbsolutePath();
            }
            
            log.warn("No files found in archive: {}", compressedFilePath);
            return compressedFilePath;
            
        } catch (IOException e) {
            log.error("Error decompressing file: {}", e.getMessage());
            return compressedFilePath;
        }
    }
    
}

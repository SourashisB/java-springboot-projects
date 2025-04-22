# java-springboot-projects
A collection of projects in Java and Spring Boot focusing on FinTech situations

25/1: Long Portfolio simulation


22/4:

Project Requirements
The CLI tool should support the following features:

Database Connectivity
    Support for Multiple DBMS: Provide support for connecting to various types of databases (e.g., MySQL, PostgreSQL, MongoDB).
    Connection Parameters: Allow users to specify database connection parameters. Parameters may include host, port, username, password, and database name.
    Connection Testing: Validate credentials based on the database type before proceeding with backup operations.
    Error Handling: Implement error handling for database connection failures.
Backup Operations
    Backup Types: Support full, incremental, and differential backup types based on the database type and user preference.
    Compression: Compress backup files to reduce storage space.
Storage Options
    Local Storage: Allow users to store backup files locally on the system.
    Cloud Storage: Provide options to store backup files on cloud storage services like AWS S3, Google Cloud Storage, or Azure Blob Storage.
Logging and Notifications
    Logging: Log backup activities, including start time, end time, status, time taken, and any errors encountered.
    Notifications: Optionally send slack notification on completion of backup operations.
Restore Operations
    Restore Backup: Implement a restore operation to recover the database from a backup file.
    Selective Restore: Provide options for selective restoration of specific tables or collections if supported by the DBMS.
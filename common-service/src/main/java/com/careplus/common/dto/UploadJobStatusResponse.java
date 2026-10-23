package com.careplus.common.dto;

import java.time.LocalDateTime;

public record UploadJobStatusResponse(
        Long jobId,
        String fileName,
        JobStatus status,
        int totalRows,
        int processedRows,
        int importedRows,
        int skippedRows,
        int failedBatches,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {}

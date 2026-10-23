package com.careplus.common.dto;

import java.time.LocalDateTime;

public record UploadBatchResponse(
		 Long batchId,
	        int batchNumber,
	        int startRow,
	        int endRow,
	        JobStatus status,
	        int totalRows,
	        int importedRows,
	        int skippedRows,
	        LocalDateTime startedAt,
	        LocalDateTime completedAt,
	        String errorMessage) {}

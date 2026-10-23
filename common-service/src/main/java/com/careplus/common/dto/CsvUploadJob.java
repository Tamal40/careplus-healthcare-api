package com.careplus.common.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="csv_upload_job")
public class CsvUploadJob implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3175070610106411342L;
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;
	
	@Column(nullable=false,name="file_name")
	private String fileName;
	
	@Column(name="entity_type")
	private String entityType;
	
	@Column(name="status")
	@Enumerated(EnumType.STRING)
	private JobStatus status;
	
	@Column(name="total_rows")
	private int totalRows;
	
	@Column(name="processes_rows")
	private int processedRows;
	
	@Column(name="imported_rows")
	private int importedRows;
	
	@Column(name="skipped_rows")
	private int skippedRows;
	
	@Column(name="failed_batches")
	private int failedBatches;
	
	@Column(name="createdAt")
	private LocalDateTime createdAt;
	
	@Column(name="started_at")
	private LocalDateTime startedAt;
	
	@Column(name="completed_at")
	private LocalDateTime completedAt;
	
	@Column(name="error_message")
	private String errorMessage;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getEntityType() {
		return entityType;
	}

	public void setEntityType(String entityType) {
		this.entityType = entityType;
	}

	public JobStatus getStatus() {
		return status;
	}

	public void setStatus(JobStatus status) {
		this.status = status;
	}

	public int getTotalRows() {
		return totalRows;
	}

	public void setTotalRows(int totalRows) {
		this.totalRows = totalRows;
	}

	public int getProcessedRows() {
		return processedRows;
	}

	public void setProcessedRows(int processedRows) {
		this.processedRows = processedRows;
	}

	public int getImportedRows() {
		return importedRows;
	}

	public void setImportedRows(int importedRows) {
		this.importedRows = importedRows;
	}

	public int getSkippedRows() {
		return skippedRows;
	}

	public void setSkippedRows(int skippedRows) {
		this.skippedRows = skippedRows;
	}

	public int getFailedBatches() {
		return failedBatches;
	}

	public void setFailedBatches(int failedBatches) {
		this.failedBatches = failedBatches;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getStartedAt() {
		return startedAt;
	}

	public void setStartedAt(LocalDateTime startedAt) {
		this.startedAt = startedAt;
	}

	public LocalDateTime getCompletedAt() {
		return completedAt;
	}

	public void setCompletedAt(LocalDateTime completedAt) {
		this.completedAt = completedAt;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	
	

}

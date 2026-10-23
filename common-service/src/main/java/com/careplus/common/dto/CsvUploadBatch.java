package com.careplus.common.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name="csv_upload_batch",
uniqueConstraints = {
    @UniqueConstraint(
        name = "uk_jobid_batchnumber",
        columnNames = {
            "job_id",
            "batch_number"
        }
    )
})
public class CsvUploadBatch implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5426340906999619597L;
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;
	
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "job_id", nullable = false)
	private CsvUploadJob jobId;
	
	@Column(name="batch_number")
	private int batchNumber;
	
	@Column(name="start_row")
	private int startRow;
	
	@Column(name="end_rows")
	private int endRow;
	
	@Column(name="status")
	@Enumerated(EnumType.STRING)
	private JobStatus status;
	
	@Column(name="total_rows")
	private int totalRows;
	
	@Column(name="imported_rows")
	private int importedRows;
	
	@Column(name="skipped_rows")
	private int skippedRows;
	
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

	public CsvUploadJob getJobId() {
		return jobId;
	}

	public void setJobId(CsvUploadJob jobId) {
		this.jobId = jobId;
	}

	public int getBatchNumber() {
		return batchNumber;
	}

	public void setBatchNumber(int batchNumber) {
		this.batchNumber = batchNumber;
	}

	public int getStartRow() {
		return startRow;
	}

	public void setStartRow(int startRow) {
		this.startRow = startRow;
	}

	public int getEndRow() {
		return endRow;
	}

	public void setEndRow(int endRow) {
		this.endRow = endRow;
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

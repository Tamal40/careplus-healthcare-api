package com.careplus.common.dto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "csv_upload_row_error")
public class CsvUploadRowError {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private CsvUploadJob job;

    @Column(name = "line_number", nullable = false)
    private int lineNumber;

    @Column(name = "error_message", nullable = false, length = 2000)
    private String errorMessage;

    public Long getId() { return id; }

    public CsvUploadJob getJob() { return job; }
    public void setJob(CsvUploadJob job) { this.job = job; }

    public int getLineNumber() { return lineNumber; }
    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}

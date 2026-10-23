package com.careplus.patient.dto;

import com.careplus.common.dto.JobStatus;

public class UploadJobResponse {
    private Long jobId;
    private String fileName;
    private JobStatus status;

    public UploadJobResponse() {
    }

    public UploadJobResponse(
            Long jobId, String fileName, JobStatus status) {
        this.jobId = jobId;
        this.fileName = fileName;
        this.status = status;
    }

    public Long getJobId() {
        return jobId;
    }

    public String getFileName() {
        return fileName;
    }

    public JobStatus getStatus() {
        return status;
    }
}

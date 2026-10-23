package com.careplus.patient.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.careplus.common.dto.CsvUploadJob;
import com.careplus.common.dto.UploadBatchResponse;
import com.careplus.common.dto.UploadJobStatusResponse;
import com.careplus.common.dto.UploadRowErrorResponse;
import com.careplus.patient.repository.CsvUploadBatchRepository;
import com.careplus.patient.repository.CsvUploadJobRepository;
import com.careplus.patient.repository.CsvUploadRowErrorRepository;

@Service
public class CsvUploadStatusService {
	
	@Autowired
	 private  CsvUploadJobRepository jobRepository;
	@Autowired
	    private  CsvUploadBatchRepository batchRepository;
	@Autowired
	    private  CsvUploadRowErrorRepository errorRepository;


	    @Transactional(readOnly = true)
	    public UploadJobStatusResponse getJobStatus(Long jobId) {

	        CsvUploadJob job = getJob(jobId);

	        return new UploadJobStatusResponse(
	                job.getId(),
	                job.getFileName(),
	                job.getStatus(),
	                job.getTotalRows(),
	                job.getProcessedRows(),
	                job.getImportedRows(),
	                job.getSkippedRows(),
	                job.getFailedBatches(),
	                job.getCreatedAt(),
	                job.getCompletedAt()
	        );
	    }

	    @Transactional(readOnly = true)
	    public List<UploadBatchResponse> getBatches(Long jobId) {

	        getJob(jobId);

	        return batchRepository
	                .findByJobId_IdOrderByBatchNumberAsc(jobId)
	                .stream()
	                .map(batch -> new UploadBatchResponse(
	                        batch.getId(),
	                        batch.getBatchNumber(),
	                        batch.getStartRow(),
	                        batch.getEndRow(),
	                        batch.getStatus(),
	                        batch.getTotalRows(),
	                        batch.getImportedRows(),
	                        batch.getSkippedRows(),
	                        batch.getStartedAt(),
	                        batch.getCompletedAt(),
	                        batch.getErrorMessage()
	                ))
	                .toList();
	    }

	    @Transactional(readOnly = true)
	    public List<UploadRowErrorResponse> getErrors(Long jobId) {

	        getJob(jobId);

	        return errorRepository
	                .findByJob_IdOrderByLineNumberAsc(jobId)
	                .stream()
	                .map(error -> new UploadRowErrorResponse(
	                        error.getId(),
	                        error.getLineNumber(),
	                        error.getErrorMessage()
	                ))
	                .toList();
	    }

	    private CsvUploadJob getJob(Long jobId) {
	        return jobRepository.findById(jobId)
	                .orElseThrow(() -> new ResponseStatusException(
	                        HttpStatus.NOT_FOUND,
	                        "Upload job not found: " + jobId
	                ));
	    }

}

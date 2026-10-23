package com.careplus.patient.service;

import java.nio.file.Path;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.careplus.common.dto.JobStatus;
import com.careplus.patient.dto.PatientUploadResponse;
import com.careplus.patient.repository.CsvUploadJobRepository;

@Service
public class PatientCsvUploadWorker {

	@Autowired
	private CsvUploadJobRepository repo;
	
	@Autowired
	private  PatientService patientService;
	
	@Async("csvUploadExecutor")
	public void processUpload(Long jobId,String filePath) {
		
		Path path = Path.of(filePath);
		
		 var job = repo.findById(jobId)
	                .orElseThrow(() ->
	                        new IllegalArgumentException(
	                                "Upload job not found: " + jobId));
		 
		  try {
	            job.setStatus(JobStatus.PROCESSING);
	            repo.save(job);
	            

	            PatientUploadResponse result =
	                    patientService.uploadFile(job,
	                            new PathMultipartFile(path));

	            job.setTotalRows(result.getTotalRows());
	            job.setProcessedRows(result.getTotalRows());
	            job.setImportedRows(result.getImported());
	            job.setSkippedRows(result.getSkipped());

	            job.setStatus(
	                    result.getErrors().isEmpty()
	                            ? JobStatus.COMPLETED
	                            : JobStatus.COMPLETED_WITH_ERRORS);

	            job.setCompletedAt(LocalDateTime.now());
	            repo.saveAndFlush(job);

	        } catch (Exception ex) {
	            job.setStatus(JobStatus.FAILED);
	            repo.save(job);
	        }
	    }
	}


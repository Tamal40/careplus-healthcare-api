package com.careplus.patient.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.careplus.common.dto.CsvUploadJob;
import com.careplus.common.dto.JobStatus;
import com.careplus.patient.dto.UploadJobResponse;
import com.careplus.patient.repository.CsvUploadJobRepository;

@Service
public class PatientCsvUploadService {
	
	@Autowired
	private  CsvUploadJobRepository jobRepository;
	
	@Autowired
    private  PatientCsvUploadWorker worker;
	
	public UploadJobResponse submit(MultipartFile file) throws IOException {
		if(file==null || file.isEmpty()) {
			  throw new IllegalArgumentException(
	                    "Please upload a non-empty CSV file");
		}
		
		 String fileName = Path.of(file.getOriginalFilename() == null
	                ? "upload.csv"
	                : file.getOriginalFilename())
	                .getFileName()
	                .toString();
		 
		 if (!fileName.toLowerCase().endsWith(".csv")) {
	            throw new IllegalArgumentException(
	                    "Only CSV files are allowed");
	        }
		 
		 Path tempFile = Files.createTempFile(
	                "patient-upload-", ".csv");
		 
		 try {
	            file.transferTo(tempFile);

	            CsvUploadJob job = new CsvUploadJob();
	            job.setFileName(fileName);
	            job.setStatus(JobStatus.STARTED);

	            job = jobRepository.saveAndFlush(job);

	            // Submit to a separate Spring bean for async execution.
	            worker.processUpload(job.getId(), tempFile.toString());

	            return new UploadJobResponse(
	                    job.getId(),
	                    job.getFileName(),
	                    job.getStatus());

	        } catch (IOException | RuntimeException ex) {
	            Files.deleteIfExists(tempFile);
	            throw ex;
	        }

	}

}

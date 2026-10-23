package com.careplus.doctor.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.careplus.common.dto.Appointment;
import com.careplus.common.dto.AppointmentDto;
import com.careplus.common.dto.Doctor;
import com.careplus.common.dto.Gender;
import com.careplus.common.dto.Patient;
import com.careplus.doctor.dto.DoctorUploadResponse;
import com.careplus.doctor.dto.UploadError;
import com.careplus.doctor.exception.DoctorExistsException;
import com.careplus.doctor.repository.DoctorRepository;



@Service
@Transactional
public class DoctorService {
	
	@Autowired
	DoctorRepository repo;
	
	private static final int BATCH_SIZE = 10;
	private static final int THREAD_COUNT = 3;
	
	@Transactional
	public List<Doctor> getAllDoctor(int offset,int limit){
		//PageRequest p=PageRequest.of(offset, limit);
		return repo.findAllDoctors(offset,limit);
	}

	@Transactional
	public Doctor getDoctor(Long id) {
		
		return repo.findById(id).orElse(null);
	}
	
@Transactional
public Doctor createDoctor(Doctor p) throws DoctorExistsException {
	    if(repo.findByEmail(p.getEmail()).isPresent()) {
	    	throw new DoctorExistsException("Dcotor already exists");
	    }
		p.setCreated_at(LocalDate.now());
		return repo.saveAndFlush(p);
	}

@Transactional
public List<AppointmentDto> getAppointments(Long id) {
	Doctor p=repo.findById(id).orElse(null);
	if(p==null) {
		return null;
	}
	List<Appointment> app=p.getAppointments();
	List<AppointmentDto> appt=new ArrayList<>();
	for(Appointment a:app) {
		AppointmentDto ap=new AppointmentDto();
		ap.setAppointmentDate(a.getAppointmentDate());
		ap.setAppointmentTime(a.getAppointmentTime());
		ap.setReason(a.getReason());
		ap.setCreated_at(a.getCreated_at());
		ap.setUpdated_at(a.getUpdated_at());
		ap.setStatus(a.getStatus().toString());
		appt.add(ap);
	}
	return appt;
	
}

private void validateHeader(String header) {

    if (header == null || header.isBlank()) {
        throw new IllegalArgumentException("CSV header is missing");
    }

    String expectedHeader =
            "name,email,specialization,active";

    if (!header.trim().equalsIgnoreCase(expectedHeader)) {
        throw new IllegalArgumentException(
                "Invalid CSV header. Expected: " + expectedHeader
        );
    }
}
@Transactional
public DoctorUploadResponse uploadFile(MultipartFile file) {
	
	List<Doctor> batch=new ArrayList<>(BATCH_SIZE);
	Set<String> emails=new HashSet<>();
	DoctorUploadResponse response = new DoctorUploadResponse();
	try(BufferedReader  reader=new BufferedReader(new
			InputStreamReader(file.getInputStream()))){
		String header=reader.readLine();
		validateHeader(header);
		String line;
		int lineNumber=1;
		while((line=reader.readLine())!=null){
			lineNumber++;
			if(line.isBlank()) {
				continue;
			}
			 response.incrementTotalRows();
			try {
				Doctor p=parseAndValidate(line
						,lineNumber,emails);
				batch.add(p);
				if(batch.size()==BATCH_SIZE) {
					saveBatch(batch,response);
				}
			}catch(Exception ex) {

                response.incrementSkipped();
                response.addError(
                    new UploadError(lineNumber, ex.getMessage())
                );
			}
			 if (!batch.isEmpty()) {
	                saveBatch(batch, response);
	            }
		}
		return response;
	}
	 catch (IOException ex) {
         throw new RuntimeException("Unable to read CSV file", ex);
     }
}


private void saveBatch(
        List<Doctor> batch,
        DoctorUploadResponse response) {

    if (batch == null || batch.isEmpty()) {
        return;
    }

    // Create a copy because the original batch may be cleared.
    List<Doctor> batchToSave = new ArrayList<>(batch);

    ExecutorService executor =
            Executors.newFixedThreadPool(THREAD_COUNT);

    try {
        Future<Integer> future = executor.submit(() -> {
            repo.saveAllAndFlush(batch);
            return batchToSave.size();
        });

        // Wait for the database save to complete.
        int savedCount = future.get();

        response.addImported(savedCount);

    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException("Batch save was interrupted", e);

    } catch (ExecutionException e) {
        throw new RuntimeException("Failed to save doctor batch", e);

    } finally {
        executor.shutdown();
    }

    batch.clear();
}


private Doctor parseAndValidate(
        String line,
        int lineNumber,
        Set<String> emailsInFile) {

    String[] columns = line.split(",", -1);

    if (columns.length != 4) {
        throw new IllegalArgumentException(
                "Expected 4 columns"
        );
    }

    String name = columns[0].trim();
    String email = columns[1].trim();
    String spcialization = columns[2].trim();
    String active = columns[3].trim();

    if (name.isBlank()) {
        throw new IllegalArgumentException("name is required");
    }

    if (email.isBlank()) {
        throw new IllegalArgumentException("email is required");
    }

    if (spcialization.isBlank()) {
        throw new IllegalArgumentException("specialization is required");
    }

    if (active.isBlank()) {
        throw new IllegalArgumentException(
                "active is required"
        );
    }

   
    

  

    if (!active.equals("true") &&
    		!active.equals("false")) {

        throw new IllegalArgumentException(
                "Active must be TRUE or FALSE"
        );
    }

    /*
     * Duplicate inside same CSV.
     */
    if (!emailsInFile.add(email.toLowerCase())) {
        throw new IllegalArgumentException(
                "duplicate email in file"
        );
    }

    /*
     * Duplicate already stored in MySQL.
     */
    if (!repo.findByEmail(email).isEmpty()) {
        throw new IllegalArgumentException(
                "email already exists"
        );
    }

    Doctor doctor = new Doctor();

    doctor.setName(name);
    doctor.setEmail(email);
    doctor.setActive(Boolean.valueOf(active));
    doctor.setSpecialization(spcialization);

    LocalDate today = LocalDate.now();

    doctor.setCreated_at(today);

    return doctor;
}


	
}

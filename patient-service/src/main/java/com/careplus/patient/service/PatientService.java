package com.careplus.patient.service;

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
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.careplus.common.dto.Appointment;
import com.careplus.common.dto.AppointmentDto;
import com.careplus.common.dto.CsvUploadBatch;
import com.careplus.common.dto.CsvUploadJob;
import com.careplus.common.dto.CsvUploadRowError;
import com.careplus.common.dto.Doctor;
import com.careplus.common.dto.Gender;
import com.careplus.common.dto.JobStatus;
import com.careplus.common.dto.MedicalRecord;
import com.careplus.common.dto.Patient;
import com.careplus.common.dto.Prescription;
import com.careplus.patient.dto.MedicalRecordRequest;
import com.careplus.patient.dto.PatientUploadResponse;
import com.careplus.patient.dto.PrescriptionRequest;
import com.careplus.patient.dto.UploadError;
import com.careplus.patient.exception.AppointmentNotFoundException;
import com.careplus.patient.exception.PatientExistsException;
import com.careplus.patient.exception.PatientNotFoundException;
import com.careplus.patient.repository.CsvUploadBatchRepository;
import com.careplus.patient.repository.CsvUploadRowErrorRepository;
import com.careplus.patient.repository.MedicalRecordRepository;
import com.careplus.patient.repository.PatientRepository;
import com.careplus.patient.repository.PrescriptionRepository;

import jakarta.persistence.EntityManager;


@Service
@Transactional
public class PatientService {
	

	@Autowired
	PatientRepository repo;
	
	@Autowired
	private MedicalRecordRepository medicalrepo;
	
	@Autowired
	private PrescriptionRepository prescriptionrepo;
	
	@Autowired
	private RestTemplate restTemplate;
	
	@Autowired
	private EntityManager entityManager;
	
	@Autowired
	private CsvUploadBatchRepository batchRepo;
	
	@Autowired
	private CsvUploadRowErrorRepository errorRepo;
	private static final int BATCH_SIZE = 10;
	private static final int THREAD_COUNT = 3;
	@Transactional
	public List<Patient> getAllPatient(int offset,int limit){
		return repo.findAllPatients(offset,limit);
	}

	/**
	 * Filtered listing. All filters are optional; age is derived from date_of_birth.
	 * offset/limit keep the same meaning as in {@link #getAllPatient(int, int)}.
	 */
	@Transactional
	public List<Patient> searchPatients(String gender, String name, Integer minAge, Integer maxAge, int offset, int limit){
		String genderValue = null;
		if (gender != null && !gender.isBlank()) {
			try {
				genderValue = Gender.valueOf(gender.trim().toUpperCase()).name();
			} catch (IllegalArgumentException ex) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "gender must be MALE or FEMALE");
			}
		}
		if ((minAge != null && minAge < 0) || (maxAge != null && maxAge < 0)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minAge and maxAge must not be negative");
		}
		if (minAge != null && maxAge != null && minAge > maxAge) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minAge must not be greater than maxAge");
		}
		LocalDate today = LocalDate.now();
		// age >= minAge  <=>  born on or before (today - minAge years)
		LocalDate dobTo = minAge == null ? null : today.minusYears(minAge);
		// age <= maxAge  <=>  born after (today - (maxAge + 1) years)
		LocalDate dobFrom = maxAge == null ? null : today.minusYears(maxAge + 1L).plusDays(1);
		String nameValue = (name == null || name.isBlank()) ? null : name.trim();
		return repo.searchPatients(genderValue, nameValue, dobFrom, dobTo, offset, limit);
	}

	@Transactional
	public Patient getPatient(Long id) {

		return repo.findById(id).orElseThrow(() -> new PatientNotFoundException(id));
	}

	/** The patient behind a login: the patient whose email is the JWT username. */
	@Transactional
	public Patient getPatientByEmail(String email) {
		if (email == null || email.isBlank()) {
			throw new PatientNotFoundException("No patient record is linked to this login");
		}
		return repo.findByEmail(email.trim())
				.orElseThrow(() -> new PatientNotFoundException("No patient record is linked to this login"));
	}
	
@Transactional
public Patient createPatient(Patient p) throws PatientExistsException {
	    if(repo.findByEmail(p.getEmail()).isPresent()) {
	    	throw new PatientExistsException("Patient already exists");
	    }
		
	    p.setCreated_at(LocalDate.now());
	    p.setUpdated_at(LocalDate.now());
		return repo.saveAndFlush(p);
	}

@Transactional
public List<AppointmentDto> getAppointments(Long id) {
	Patient p=repo.findById(id).orElseThrow(() -> new PatientNotFoundException(id));
	List<Appointment> app=p.getAppointments();
	List<AppointmentDto> appt=new ArrayList<>();
	for(Appointment a:app) {
		AppointmentDto ap=new AppointmentDto();
		// id and doctor_id let a client pick one appointment, e.g. to cancel it.
		ap.setId(a.getId());
		ap.setPatient_id(p.getId());
		ap.setDoctor_id(a.getDoctor() != null ? a.getDoctor().getId() : null);
		ap.setAppointmentDate(a.getAppointmentDate());
		ap.setAppointmentTime(a.getAppointmentTime());
		ap.setReason(a.getReason());
		ap.setCreated_at(a.getCreated_at());
		ap.setUpdated_at(a.getUpdated_at());
		// Status is nullable on the entity; calling toString() on it threw for appointments without one.
		ap.setStatus(a.getStatus() != null ? a.getStatus().name() : null);
		appt.add(ap);
	}
	return appt;
	
}

@Transactional
public MedicalRecordRequest createRecord(MedicalRecordRequest record,String auth) {
	
	HttpHeaders headers=new HttpHeaders();
	headers.set(HttpHeaders.AUTHORIZATION, auth);
	HttpEntity<Void> entity=new HttpEntity<>(headers);
	AppointmentDto appointment;
	try {
		appointment = restTemplate.exchange(
		        "http://localhost:8083/api/appointments/"
		                + record.getAppointment_id(),
		        HttpMethod.GET,
		        entity,
		        AppointmentDto.class
		).getBody();
	} catch (HttpClientErrorException.NotFound ex) {
		// appointment-service answers 404 for an unknown id; report it as such rather than as a 500.
		throw new AppointmentNotFoundException(record.getAppointment_id());
	}

	if (appointment == null) {
	    throw new AppointmentNotFoundException(record.getAppointment_id());
	}
	MedicalRecord rec = new MedicalRecord();

	rec.setAppointment(entityManager.getReference(Appointment.class,appointment.getId()));
	rec.setPatient(entityManager.getReference(Patient.class,appointment.getPatient_id()));
	rec.setDoctor(entityManager.getReference(Doctor.class,appointment.getDoctor_id()));

	rec.setDiagnosis(record.getDiagnosis());
	rec.setNotes(record.getNotes());
	rec.setCreated_at(LocalDateTime.now());

	MedicalRecord recc=medicalrepo.save(rec);
	
	// Prescriptions are optional: a medical record without any used to throw a NullPointerException here,
	// after the record itself had already been saved.
	List<PrescriptionRequest> prescriptions = record.getPrescriptions() != null ? record.getPrescriptions() : List.of();
	for (PrescriptionRequest p : prescriptions) {

	    Prescription prescription = new Prescription();

	    prescription.setMedicalRecord(entityManager.getReference(MedicalRecord.class,recc.getId()));
	    prescription.setMedicine_name(p.getMedicine_name());
	    prescription.setDosage(p.getDosage());
	    prescription.setFrequency(p.getFrequency());
	    prescription.setDuration(p.getDuration());

	    prescriptionrepo.save(prescription);
	}
	return record;
}

public List<MedicalRecordRequest> getMedicalHistory(Long id) {
	// Spring Data returns an empty list, never null, so a patient with no history simply gets [].
	List<MedicalRecord> med=medicalrepo.findByPatient_Id(id);
	List<MedicalRecordRequest> rec=new ArrayList<>();
	for(MedicalRecord reco:med) {
		MedicalRecordRequest req=new MedicalRecordRequest();
		req.setDiagnosis(reco.getDiagnosis());
		req.setNotes(reco.getNotes());
		req.setAppointment_id(reco.getAppointment().getId());
		List<Prescription> pres=prescriptionrepo.findByid(reco.getId());
		List<PrescriptionRequest> presc=new ArrayList<>();
		if(pres!=null) {
			pres.forEach(p->{
				PrescriptionRequest p1=new PrescriptionRequest();
				p1.setDosage(p.getDosage());
				p1.setDuration(p.getDuration());
				p1.setFrequency(p.getFrequency());
				p1.setMedicine_name(p.getMedicine_name());
				presc.add(p1);
				
			});
		}
		req.setPrescriptions(presc);
		rec.add(req);
		
	}
	return rec;
	
}

private void validateHeader(String header) {

    if (header == null || header.isBlank()) {
        throw new IllegalArgumentException("CSV header is missing");
    }

    String expectedHeader =
            "name,email,phone,date_of_birth,gender";

    if (!header.trim().equalsIgnoreCase(expectedHeader)) {
        throw new IllegalArgumentException(
                "Invalid CSV header. Expected: " + expectedHeader
        );
    }
}

@Transactional
public PatientUploadResponse uploadFile(CsvUploadJob job,MultipartFile file) {
	
	List<Patient> batch=new ArrayList<>(BATCH_SIZE);
	Set<String> emails=new HashSet<>();
	PatientUploadResponse response = new PatientUploadResponse();
	int batchNumber = 0;
	int batchTotalRows = 0;
	int batchImportedRows = 0;
	int batchSkippedRows = 0;

	int batchStartRow = 2;
	int endRow=0;
	try(BufferedReader  reader=new BufferedReader(new
			InputStreamReader(file.getInputStream()))){
		String header=reader.readLine();
		validateHeader(header);
		String line;
		int lineNumber=1;
		while((line=reader.readLine())!=null){
			lineNumber++;
			endRow = lineNumber;
			if(line.isBlank()) {
				continue;
			}
			batchTotalRows++;
			 response.incrementTotalRows();
			try {
				Patient p=parseAndValidate(line
						,lineNumber,emails);
				batch.add(p);
				batchImportedRows++;
				if(batch.size()==BATCH_SIZE) {
					saveBatch(batch,response);
				}
			}catch(Exception ex) {

                response.incrementSkipped();
                batchSkippedRows++;
                CsvUploadRowError error=new CsvUploadRowError();
                error.setErrorMessage(ex.getMessage());
                error.setJob(job);
                error.setLineNumber(lineNumber);
                errorRepo.save(error);
                response.addError(
                    new UploadError(lineNumber, ex.getMessage())
                );
			}
			 if (!batch.isEmpty()) {
	                saveBatch(batch, response);
	            }
			 CsvUploadBatch batchupload = new CsvUploadBatch();

			 batchupload.setJobId(job);
			 batchupload.setBatchNumber(batchNumber);
			 batchupload.setStartRow(batchStartRow);
			 batchupload.setEndRow(endRow);
			 batchupload.setTotalRows(batchTotalRows);
			 batchupload.setImportedRows(batchImportedRows);
			 batchupload.setSkippedRows(batchSkippedRows);

			 batchupload.setStatus(
					 batchSkippedRows == 0
			            ? JobStatus.COMPLETED
			            : JobStatus.COMPLETED_WITH_ERRORS
			    );

			 batchupload.setStartedAt(LocalDateTime.now());
			 batchupload.setCompletedAt(LocalDateTime.now());

			    batchRepo.save(batchupload);
			    batchStartRow = lineNumber + 1;
			    batchNumber++;
			    batchSkippedRows=0;
		}
		return response;
	}
	 catch (IOException ex) {
         throw new RuntimeException("Unable to read CSV file", ex);
     }
}


private void saveBatch(
        List<Patient> batch,
        PatientUploadResponse response) {

    if (batch == null || batch.isEmpty()) {
        return;
    }

    // Create a copy because the original batch may be cleared.
    List<Patient> batchToSave = new ArrayList<>(batch);

    ExecutorService executor =
            Executors.newFixedThreadPool(THREAD_COUNT);

    try {
        Future<Integer> future = executor.submit(() -> {
            repo.saveAll(batchToSave);
            return batchToSave.size();
        });

        // Wait for the database save to complete.
        int savedCount = future.get();

        response.addImported(savedCount);

    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        CsvUploadBatch uploadbatch=new CsvUploadBatch();
        uploadbatch.setStatus(JobStatus.FAILED);
    	uploadbatch.setCreatedAt(LocalDateTime.now());
    	uploadbatch.setErrorMessage("Batch save was interrupted");
    	batchRepo.save(uploadbatch);
        throw new RuntimeException("Batch save was interrupted", e);

    } catch (ExecutionException e) {
    	CsvUploadBatch uploadbatch=new CsvUploadBatch();
    	uploadbatch.setStatus(JobStatus.FAILED);
    	uploadbatch.setCreatedAt(LocalDateTime.now());
    	uploadbatch.setErrorMessage("Failed to save patient batch");
    	batchRepo.save(uploadbatch);
        throw new RuntimeException("Failed to save patient batch", e);

    } finally {
        executor.shutdown();
    }

    batch.clear();
}


private Patient parseAndValidate(
        String line,
        int lineNumber,
        Set<String> emailsInFile) {

    String[] columns = line.split(",", -1);

    if (columns.length != 5) {
        throw new IllegalArgumentException(
                "Expected 5 columns"
        );
    }

    String name = columns[0].trim();
    String email = columns[1].trim();
    String phone = columns[2].trim();
    String dobValue = columns[3].trim();
    String genderValue = columns[4].trim();

    if (name.isBlank()) {
        throw new IllegalArgumentException("name is required");
    }

    if (email.isBlank()) {
        throw new IllegalArgumentException("email is required");
    }

    if (phone.isBlank()) {
        throw new IllegalArgumentException("phone is required");
    }

    if (dobValue.isBlank()) {
        throw new IllegalArgumentException(
                "date_of_birth is required"
        );
    }

    LocalDate dob;

    try {
        dob = LocalDate.parse(dobValue);
    } catch (DateTimeParseException ex) {
        throw new IllegalArgumentException(
                "invalid date_of_birth"
        );
    }

    if (dob.isAfter(LocalDate.now())) {
        throw new IllegalArgumentException(
                "date_of_birth cannot be in the future"
        );
    }

    Gender gender;

    try {
        gender = Gender.valueOf(
                genderValue.toUpperCase()
        );
    } catch (IllegalArgumentException ex) {
        throw new IllegalArgumentException(
                "gender must be MALE or FEMALE"
        );
    }

    if (gender != Gender.MALE &&
        gender != Gender.FEMALE) {

        throw new IllegalArgumentException(
                "gender must be MALE or FEMALE"
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

    Patient patient = new Patient();

    patient.setName(name);
    patient.setEmail(email);
    patient.setPhone(phone);
    patient.setDate_of_birth(dob);
    patient.setGender(gender);

    LocalDate today = LocalDate.now();

    patient.setCreated_at(today);
    patient.setUpdated_at(today);

    return patient;
}

}

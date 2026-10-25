package com.careplus.patient.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.careplus.common.dto.Patient;


@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
	
	 Optional<Patient> findByEmail(String email);

	 @Query(value="SELECT * FROM patient LIMIT :limit OFFSET :offset",nativeQuery=true)
	 List<Patient> findAllPatients(@Param("offset") int offset,@Param("limit") int limit);

	 // Every filter is optional: a null parameter matches all rows. Age is passed as a date_of_birth range.
	 @Query(value="SELECT * FROM patient "
	 		+ "WHERE (:gender IS NULL OR gender = :gender) "
	 		+ "AND (:name IS NULL OR LOWER(name) LIKE LOWER(CONCAT('%', :name, '%'))) "
	 		+ "AND (:dobFrom IS NULL OR date_of_birth >= :dobFrom) "
	 		+ "AND (:dobTo IS NULL OR date_of_birth <= :dobTo) "
	 		+ "ORDER BY id LIMIT :limit OFFSET :offset",nativeQuery=true)
	 List<Patient> searchPatients(@Param("gender") String gender,
			 @Param("name") String name,
			 @Param("dobFrom") LocalDate dobFrom,
			 @Param("dobTo") LocalDate dobTo,
			 @Param("offset") int offset,@Param("limit") int limit);

}

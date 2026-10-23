package com.careplus.doctor.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.careplus.common.dto.Doctor;


public interface DoctorRepository extends JpaRepository<Doctor, Long> {
	
	Optional<Doctor> findByEmail(String email);

    @Query(value = "SELECT * FROM doctor LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Doctor> findAllDoctors(
            @Param("offset") int offset,
            @Param("limit") int limit
    );

}

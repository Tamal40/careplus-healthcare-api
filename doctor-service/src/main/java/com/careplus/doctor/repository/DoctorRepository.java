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

    // Every filter is optional: a null parameter matches all rows.
    @Query(value = "SELECT * FROM doctor "
            + "WHERE (:specialization IS NULL OR LOWER(specialization) LIKE LOWER(CONCAT('%', :specialization, '%'))) "
            + "AND (:name IS NULL OR LOWER(name) LIKE LOWER(CONCAT('%', :name, '%'))) "
            + "AND (:active IS NULL OR active = :active) "
            + "ORDER BY id LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Doctor> searchDoctors(
            @Param("specialization") String specialization,
            @Param("name") String name,
            @Param("active") Boolean active,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

}

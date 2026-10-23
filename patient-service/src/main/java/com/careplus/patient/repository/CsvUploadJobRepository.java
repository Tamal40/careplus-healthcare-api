package com.careplus.patient.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.careplus.common.dto.CsvUploadJob;

public interface CsvUploadJobRepository extends JpaRepository<CsvUploadJob, Long> {

}

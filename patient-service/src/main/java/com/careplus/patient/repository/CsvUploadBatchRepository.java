package com.careplus.patient.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.careplus.common.dto.CsvUploadBatch;

public interface CsvUploadBatchRepository
extends JpaRepository<CsvUploadBatch, Long> {

List<CsvUploadBatch> findByJobId_IdOrderByBatchNumberAsc(Long jobId);
}

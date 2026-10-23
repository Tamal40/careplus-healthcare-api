package com.careplus.patient.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.careplus.common.dto.CsvUploadRowError;

public interface CsvUploadRowErrorRepository
extends JpaRepository<CsvUploadRowError, Long> {

List<CsvUploadRowError> findByJob_IdOrderByLineNumberAsc(Long jobId);
}
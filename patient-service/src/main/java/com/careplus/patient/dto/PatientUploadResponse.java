package com.careplus.patient.dto;

import java.util.ArrayList;
import java.util.List;

public class PatientUploadResponse {

    private int totalRows;
    private int imported;
    private int skipped;

    private List<UploadError> errors = new ArrayList<>();

    public PatientUploadResponse() {
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getImported() {
        return imported;
    }

    public void setImported(int imported) {
        this.imported = imported;
    }

    public int getSkipped() {
        return skipped;
    }

    public void setSkipped(int skipped) {
        this.skipped = skipped;
    }

    public List<UploadError> getErrors() {
        return errors;
    }

    public void setErrors(List<UploadError> errors) {
        this.errors = errors;
    }

    // Helper methods for CSV processing

    public void incrementTotalRows() {
        this.totalRows++;
    }

    public void incrementSkipped() {
        this.skipped++;
    }

    public void addImported(int count) {
        this.imported += count;
    }

    public void addError(UploadError error) {
        this.errors.add(error);
    }
}

package com.careplus.patient.dto;

public class UploadError {

    private int line;
    private String message;

    public UploadError() {
    }

    public UploadError(int line, String message) {
        this.line = line;
        this.message = message;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

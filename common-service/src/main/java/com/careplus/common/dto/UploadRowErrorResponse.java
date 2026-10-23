package com.careplus.common.dto;

public record UploadRowErrorResponse( Long errorId,
        int lineNumber,
        String errorMessage) {

}

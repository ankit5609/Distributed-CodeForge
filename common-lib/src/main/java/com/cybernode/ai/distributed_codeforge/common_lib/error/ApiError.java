package com.cybernode.ai.distributed_codeforge.common_lib.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

public record ApiError(
        HttpStatus status,
        String message,
        Instant timeStamp,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) List<ApiFieldError> errors
) {
    public ApiError(HttpStatus status, String message){
        this(status,message,Instant.now(),null);
    }
    public ApiError(HttpStatus status, String message,List<ApiFieldError> errors){
        this(status,message,Instant.now(),errors);
    }

}

record ApiFieldError(
        String field,
        String message
){

}

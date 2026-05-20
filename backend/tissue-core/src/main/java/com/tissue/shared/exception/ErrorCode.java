package com.tissue.shared.exception;

import org.springframework.http.HttpStatus;

public interface ErrorCode {

    String name();

    String getDefaultMessage();

    /**
     * The HTTP status this code maps to.
     */
    default HttpStatus getHttpStatus() {
        throw new UnsupportedOperationException("ErrorCode " + name() + " needs to provide a HttpStatus");
    }
}

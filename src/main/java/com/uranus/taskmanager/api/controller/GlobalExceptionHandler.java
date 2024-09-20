package com.uranus.taskmanager.api.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ProblemDetail> unexpectedException(Exception exception) {

		log.error("Unexpected Exception occurred: {}", exception.getMessage());

		ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		problemDetail.setTitle("Unexpected Exception");

		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(problemDetail);
	}

	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<ProblemDetail> unexpectedRuntimeException(RuntimeException exception) {

		log.error("Unexpected RuntimeException occurred: {}", exception.getMessage());

		ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		problemDetail.setTitle("Unexpected RuntimeException");

		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(problemDetail);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ProblemDetail> handleValidationException(MethodArgumentNotValidException exception) {

		log.error("Request Validation Failed: {}", exception.getMessage());

		BindingResult bindingResult = exception.getBindingResult();
		Map<String, String> fieldErrors = new HashMap<>();

		for (FieldError fieldError : bindingResult.getFieldErrors()) {
			fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
		}

		ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		problemDetail.setTitle("Field Validation Error");
		problemDetail.setDetail("One or more fields have validation errors");
		problemDetail.setProperty("fieldErrors", fieldErrors);

		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(problemDetail);
	}

}

package com.tissue.api.global;

import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.common.dto.FieldErrorDto;
import com.tissue.api.common.exception.TissueException;
import com.tissue.api.common.exception.type.ExternalServiceException;
import com.tissue.api.common.exception.type.InternalServerException;
import com.tissue.api.security.authentication.exception.JwtAuthenticationException;
import com.tissue.api.security.authentication.exception.JwtCreationException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	@ExceptionHandler(Exception.class)
	public ApiResponse<Void> unexpectedException(Exception e) {
		log.error("Unexpected exception: {}", e.getMessage(), e);

		return ApiResponse.failWithNoContent(HttpStatus.INTERNAL_SERVER_ERROR, "A unexpected problem has occured.");
	}

	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ApiResponse<List<FieldErrorDto>> handleValidationException(MethodArgumentNotValidException e) {
		log.info("Validation failed: {} ", e.getMessage(), e);

		List<FieldErrorDto> errors = extractFieldErrors(e.getBindingResult());

		return ApiResponse.fail(HttpStatus.BAD_REQUEST, "One or more fields have failed validation.", errors);
	}

	private List<FieldErrorDto> extractFieldErrors(BindingResult bindingResult) {
		return bindingResult.getFieldErrors().stream()
			.map(this::toFieldErrorDto)
			.toList();
	}

	private FieldErrorDto toFieldErrorDto(FieldError error) {
		String rejectedValue = Objects.toString(error.getRejectedValue(), "");
		return new FieldErrorDto(error.getField(), rejectedValue, error.getDefaultMessage());
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ApiResponse<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
		// enum 타입 변환 실패로 인한 예외인 경우
		if (e.getCause() instanceof InvalidFormatException) {
			log.warn("Invalid enum value provided: {} ", e.getMessage(), e);
			return ApiResponse.failWithNoContent(HttpStatus.BAD_REQUEST, "Invalid enum value provided.");
		}

		// 그 외의 요청 본문 관련 예외
		log.warn("Invalid request body provided: {} ", e.getMessage(), e);
		return ApiResponse.failWithNoContent(HttpStatus.BAD_REQUEST, "Invalid request body.");
	}

	@ExceptionHandler(JwtAuthenticationException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	public ResponseEntity<ApiResponse<Void>> handleJwtAuth(JwtAuthenticationException e) {
		log.warn("JWT authentication error: {}", e.getMessage(), e);

		HttpStatus httpStatus = e.getHttpStatus();

		return ResponseEntity
			.status(httpStatus)
			.body(ApiResponse.failWithNoContent(httpStatus, e.getMessage()));
	}

	@ExceptionHandler(JwtCreationException.class)
	public ResponseEntity<ApiResponse<Void>> handleJwtCreation(JwtCreationException e) {
		log.error("JWT creation failed: {}", e.getMessage(), e);

		HttpStatus httpStatus = e.getHttpStatus();

		return ResponseEntity
			.status(httpStatus)
			.body(ApiResponse.failWithNoContent(httpStatus, e.getMessage()));
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ApiResponse<Void>> handleMissingParam(MissingServletRequestParameterException e) {
		log.warn("Missing request parameter: {}", e.getParameterName());

		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(ApiResponse.failWithNoContent(HttpStatus.BAD_REQUEST, "Missing parameter: " + e.getParameterName()));
	}

	@ExceptionHandler(InternalServerException.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public ResponseEntity<ApiResponse<Void>> handleInternalServerException(InternalServerException e) {
		log.error("Internal server problem: {}", e.getMessage(), e);

		HttpStatus httpStatus = e.getHttpStatus();

		return ResponseEntity
			.status(httpStatus)
			.body(ApiResponse.failWithNoContent(httpStatus, e.getMessage()));
	}

	@ExceptionHandler(ExternalServiceException.class)
	public ResponseEntity<ApiResponse<Void>> handleExternalServiceException(ExternalServiceException e) {
		log.error("External service problem: {}", e.getMessage(), e);

		HttpStatus httpStatus = e.getHttpStatus();

		return ResponseEntity
			.status(httpStatus)
			.body(ApiResponse.failWithNoContent(httpStatus, e.getMessage()));
	}

	@ExceptionHandler(TissueException.class)
	public ResponseEntity<ApiResponse<Void>> handleTissueException(TissueException e) {
		log.info("Tissue exception: {}", e.getMessage(), e);

		String message = e.getMessage();
		HttpStatus httpStatus = e.getHttpStatus();

		return ResponseEntity
			.status(httpStatus)
			.body(ApiResponse.failWithNoContent(httpStatus, message));
	}
}

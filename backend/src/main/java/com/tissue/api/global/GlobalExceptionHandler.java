package com.tissue.api.global;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
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
import com.tissue.api.common.exception.type.FieldValidationException;
import com.tissue.api.common.exception.type.InternalServerException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * TODO: 커스텀 API 코드와 그 메세지를 enum으로 정의해서 사용
 *   - 어차피 Http 상태 코드는 헤더를 통해 설정.
 *   - ApiResponse의 필드명 code -> apiCode로 변경
 *   - apiCode에 커스텀 API 코드를 넣기
 *   - apiCode를 예외를 통해 전달하면 될 듯
 *   - apiCode에 맞는 메세지도 같이 정의해놓고, 해당 메세지를 응답 메세지에 사용(예외 메세지를 그대로 응답 메세지에 사용하지 말자)
 *   - apiCode의 메세지에 대한 국제화를 고려하자(예외 메세지는 그냥 영어로 유지)
 */
@SuppressWarnings("checkstyle:ParameterName")
@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	@ExceptionHandler(Exception.class)
	public ApiResponse<Void> unexpectedException(Exception ex) {
		log.error("Unexpected exception: {}", ex.getMessage(), ex);

		return ApiResponse.failWithNoContent(HttpStatus.INTERNAL_SERVER_ERROR, "A unexpected problem has occured.");
	}

	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ExceptionHandler(FieldValidationException.class)
	public ApiResponse<List<FieldErrorDto>> handleFieldValidationException(FieldValidationException ex) {
		log.info("Schema validation for custom fields failed: {}", ex.getMessage(), ex);
		return ApiResponse.fail(ex.getHttpStatus(), ex.getMessage(), ex.getFieldErrors());
	}

	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ApiResponse<List<FieldErrorDto>> handleValidationException(MethodArgumentNotValidException ex) {
		log.info("Validation failed: {} ", ex.getMessage(), ex);
		List<FieldErrorDto> errors = FieldErrorDto.fromBindingResult(ex.getBindingResult());

		return ApiResponse.fail(HttpStatus.BAD_REQUEST, "One or more fields have failed validation.", errors);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ApiResponse<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
		// enum type failure
		if (ex.getCause() instanceof InvalidFormatException) {
			log.warn("Invalid enum value provided: {} ", ex.getMessage(), ex);
			return ApiResponse.failWithNoContent(HttpStatus.BAD_REQUEST, "Invalid enum value provided.");
		}

		log.warn("Invalid request body provided: {} ", ex.getMessage(), ex);
		return ApiResponse.failWithNoContent(HttpStatus.BAD_REQUEST, "Invalid request body.");
	}

	// @ExceptionHandler(JwtAuthenticationException.class)
	// public ResponseEntity<ApiResponse<Void>> handleJwtAuth(JwtAuthenticationException ex) {
	// 	log.warn("JWT authentication error: {}", ex.getMessage(), ex);
	//
	// 	HttpStatus httpStatus = HttpStatus.UNAUTHORIZED;
	//
	// 	return ResponseEntity
	// 		.status(httpStatus)
	// 		.body(ApiResponse.failWithNoContent(httpStatus, ex.getMessage()));
	// }
	//
	// @ExceptionHandler(JwtCreationException.class)
	// public ResponseEntity<ApiResponse<Void>> handleJwtCreation(JwtCreationException ex) {
	// 	log.error("JWT creation failed: {}", ex.getMessage(), ex);
	//
	// 	HttpStatus httpStatus = ex.getHttpStatus();
	//
	// 	return ResponseEntity
	// 		.status(httpStatus)
	// 		.body(ApiResponse.failWithNoContent(httpStatus, ex.getMessage()));
	// }

	// DB constraint violation (null/unique/fk/etc)
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
		// TODO: Branch HttpStatus on cause
		log.warn("Data integrity violation: {}", ex.getMessage(), ex);

		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
			.body(ApiResponse.failWithNoContent(HttpStatus.UNPROCESSABLE_ENTITY, "Data integrity violation."));
	}

	// TODO: Handle OptimisticLockException

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ApiResponse<Void>> handleMissingParam(MissingServletRequestParameterException ex) {
		log.warn("Missing request parameter: {}", ex.getParameterName());

		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(ApiResponse.failWithNoContent(HttpStatus.BAD_REQUEST, "Missing parameter: " + ex.getParameterName()));
	}

	@ExceptionHandler(InternalServerException.class)
	public ResponseEntity<ApiResponse<Void>> handleInternalServerException(InternalServerException ex) {
		log.error("Internal server problem: {}", ex.getMessage(), ex);

		HttpStatus httpStatus = ex.getHttpStatus();

		return ResponseEntity
			.status(httpStatus)
			.body(ApiResponse.failWithNoContent(httpStatus, ex.getMessage()));
	}

	@ExceptionHandler(ExternalServiceException.class)
	public ResponseEntity<ApiResponse<Void>> handleExternalServiceException(ExternalServiceException ex) {
		log.error("External service problem: {}", ex.getMessage(), ex);

		HttpStatus httpStatus = ex.getHttpStatus();

		return ResponseEntity
			.status(httpStatus)
			.body(ApiResponse.failWithNoContent(httpStatus, ex.getMessage()));
	}

	@ExceptionHandler(TissueException.class)
	public ResponseEntity<ApiResponse<Void>> handleTissueException(TissueException ex) {
		log.info("Tissue exception: {}", ex.getMessage(), ex);

		String message = ex.getMessage();
		HttpStatus httpStatus = ex.getHttpStatus();

		return ResponseEntity
			.status(httpStatus)
			.body(ApiResponse.failWithNoContent(httpStatus, message));
	}
}

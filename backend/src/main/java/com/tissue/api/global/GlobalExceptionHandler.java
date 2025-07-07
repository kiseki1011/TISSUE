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
	public ResponseEntity<ApiResponse<Void>> handleJwtAuth(JwtAuthenticationException e) {
		log.warn("JWT authentication error: {}", e.getMessage(), e);

		HttpStatus httpStatus = HttpStatus.UNAUTHORIZED;

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

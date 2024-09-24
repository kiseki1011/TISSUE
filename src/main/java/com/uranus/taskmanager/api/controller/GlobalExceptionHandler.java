package com.uranus.taskmanager.api.controller;

import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.uranus.taskmanager.api.common.ApiResponse;
import com.uranus.taskmanager.api.common.FieldErrorDto;
import com.uranus.taskmanager.api.exception.AuthenticationException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	@ExceptionHandler(Exception.class)
	public ApiResponse<Void> unexpectedException(Exception exception) {
		log.error("Unexpected Exception: ", exception);

		return ApiResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR,
			"A unexpected problem has occured",
			null);
	}

	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	@ExceptionHandler(RuntimeException.class)
	public ApiResponse<Void> unexpectedRuntimeException(RuntimeException exception) {
		log.error("Unexpected RuntimeException :", exception);

		return ApiResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR,
			"A unexpected problem has occured",
			null);
	}

	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ApiResponse<List<FieldErrorDto>> handleValidationException(MethodArgumentNotValidException exception) {
		log.error("Field Validation Failed: ", exception);

		/*
		 * Todo: data안에 검증을 실패한 필드를 다음 처럼 표현하는 것이 과연 좋은가?
		 * "data": {
		 *    {
		 *      "field" : "***",
		 * 		"rejectedValue" : "***",
		 * 		"message" : "***"
		 *    },
		 * 	  ...
		 * }
		 */
		BindingResult bindingResult = exception.getBindingResult();
		List<FieldErrorDto> errors = bindingResult.getFieldErrors().stream()
			.map(error -> new FieldErrorDto(
				error.getField(),
				Objects.toString(error.getRejectedValue(), ""), // null이면 빈 문자열 처리
				error.getDefaultMessage()
			))
			.toList();

		/*
		 * Todo: 아래 방법을 다시 고려
		 */
		// Map<String, String> fieldErrors = new HashMap<>();
		//
		// for (FieldError fieldError : bindingResult.getFieldErrors()) {
		// 	fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
		// }

		return ApiResponse.fail(HttpStatus.BAD_REQUEST,
			"One or more fields have validation errors",
			errors);
	}

	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	@ExceptionHandler(AuthenticationException.class)
	public ApiResponse<?> handleAuthenticationException(AuthenticationException exception) {
		log.error("Authentication Related Exception: ", exception);

		return ApiResponse.fail(exception.getHttpStatus(),
			exception.getMessage(),
			null); // Todo: AuthenticationException을 상속받은 예외 클래스에 잘못된 필드를 넘기는 것을 고려(이후 data에 넘기기)
	}
}

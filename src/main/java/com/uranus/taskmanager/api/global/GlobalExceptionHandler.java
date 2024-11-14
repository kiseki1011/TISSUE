package com.uranus.taskmanager.api.global;

import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.uranus.taskmanager.api.common.ApiResponse;
import com.uranus.taskmanager.api.common.FieldErrorDto;
import com.uranus.taskmanager.api.common.exception.AuthenticationException;
import com.uranus.taskmanager.api.common.exception.InvitationException;
import com.uranus.taskmanager.api.common.exception.MemberException;
import com.uranus.taskmanager.api.common.exception.WorkspaceException;
import com.uranus.taskmanager.api.common.exception.WorkspaceMemberException;
import com.uranus.taskmanager.api.security.authorization.exception.AuthorizationException;

import lombok.extern.slf4j.Slf4j;

/**
 * Todo
 *  - AOP를 사용한 로깅 적용
 *  - 중복 로직 제거: CommonException을 처리하는 핸들러로 수정
 */
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
		log.error("Unexpected RuntimeException:", exception);

		return ApiResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR,
			"A unexpected problem has occured",
			null);
	}

	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ApiResponse<List<FieldErrorDto>> handleValidationException(MethodArgumentNotValidException exception) {
		log.error("Validation Exception: ", exception);

		List<FieldErrorDto> errors = extractFieldErrors(exception.getBindingResult());

		return ApiResponse.fail(HttpStatus.BAD_REQUEST,
			"One or more fields have validation errors",
			errors);
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

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ApiResponse<?>> handleAuthenticationException(AuthenticationException exception) {
		log.error("Authentication Related Exception: ", exception);

		HttpStatus httpStatus = exception.getHttpStatus();
		return ResponseEntity.status(httpStatus)
			.body(ApiResponse.fail(httpStatus, exception.getMessage(), null));
		// Todo: AuthenticationException을 상속받은 예외 클래스에 잘못된 필드를 넘기는 것을 고려(ApiResponse의 data에 넘기기)
	}

	@ExceptionHandler(MemberException.class)
	public ResponseEntity<ApiResponse<?>> handleMemberException(MemberException exception) {
		log.error("Member Related Exception: ", exception);

		HttpStatus httpStatus = exception.getHttpStatus();
		return ResponseEntity.status(httpStatus)
			.body(ApiResponse.fail(httpStatus, exception.getMessage(), null));
	}

	@ExceptionHandler(WorkspaceException.class)
	public ResponseEntity<ApiResponse<?>> handleWorkspaceException(WorkspaceException exception) {
		log.error("Workspace Related Exception: ", exception);

		HttpStatus httpStatus = exception.getHttpStatus();
		return ResponseEntity.status(httpStatus)
			.body(ApiResponse.fail(httpStatus, exception.getMessage(), null));
	}

	@ExceptionHandler(WorkspaceMemberException.class)
	public ResponseEntity<ApiResponse<?>> handleWorkspaceMemberException(WorkspaceMemberException exception) {
		log.error("WorkspaceMember Related Exception: ", exception);

		HttpStatus httpStatus = exception.getHttpStatus();
		return ResponseEntity.status(httpStatus)
			.body(ApiResponse.fail(httpStatus, exception.getMessage(), null));
	}

	@ExceptionHandler(InvitationException.class)
	public ResponseEntity<ApiResponse<?>> handleInvitationException(InvitationException exception) {
		log.error("Invitation Related Exception: ", exception);

		HttpStatus httpStatus = exception.getHttpStatus();
		return ResponseEntity.status(httpStatus)
			.body(ApiResponse.fail(httpStatus, exception.getMessage(), null));
	}

	@ExceptionHandler(AuthorizationException.class)
	public ResponseEntity<ApiResponse<?>> handleAuthorizationException(AuthorizationException exception) {
		log.error("Authorization Related Exception: ", exception);

		HttpStatus httpStatus = exception.getHttpStatus();
		return ResponseEntity.status(httpStatus)
			.body(ApiResponse.fail(httpStatus, exception.getMessage(), null));
	}
}

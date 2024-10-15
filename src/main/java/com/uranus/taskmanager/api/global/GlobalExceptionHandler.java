package com.uranus.taskmanager.api.global;

import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.uranus.taskmanager.api.authentication.exception.AuthenticationException;
import com.uranus.taskmanager.api.common.ApiResponse;
import com.uranus.taskmanager.api.common.FieldErrorDto;
import com.uranus.taskmanager.api.invitation.exception.InvitationException;
import com.uranus.taskmanager.api.member.exception.MemberException;
import com.uranus.taskmanager.api.workspace.exception.WorkspaceException;
import com.uranus.taskmanager.api.workspacemember.authorization.exception.AuthorizationException;
import com.uranus.taskmanager.api.workspacemember.exception.WorkspaceMemberException;

import lombok.extern.slf4j.Slf4j;

/**
 * Todo: 중복, 가독성 리팩토링
 * 	- 커스텀 비즈니스 예외에 대한 핸들링은 로그 처리를 제외하고는 중복된다.
 * 	- 아예 CommonException을 잡고, 로그 처리만 예외 별로 다르게 처리되도록 구현?
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
		log.error("Field Validation Failed: ", exception);

		// Todo: 가독성을 위해 리팩토링(메서드 추출)
		BindingResult bindingResult = exception.getBindingResult();
		List<FieldErrorDto> errors = bindingResult.getFieldErrors().stream()
			.map(error -> new FieldErrorDto(
				error.getField(),
				Objects.toString(error.getRejectedValue(), ""),
				error.getDefaultMessage()
			))
			.toList();

		return ApiResponse.fail(HttpStatus.BAD_REQUEST,
			"One or more fields have validation errors",
			errors);
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ApiResponse<?>> handleAuthenticationException(AuthenticationException exception) {
		log.error("Authentication Related Exception: ", exception);

		return ResponseEntity.status(exception.getHttpStatus())
			.body(ApiResponse.fail(exception.getHttpStatus(), exception.getMessage(), null));
		// Todo: AuthenticationException을 상속받은 예외 클래스에 잘못된 필드를 넘기는 것을 고려(ApiResponse의 data에 넘기기)
	}

	@ExceptionHandler(MemberException.class)
	public ResponseEntity<ApiResponse<?>> handleMemberException(MemberException exception) {
		log.error("Member Related Exception: ", exception);

		return ResponseEntity.status(exception.getHttpStatus())
			.body(ApiResponse.fail(exception.getHttpStatus(), exception.getMessage(), null));
	}

	@ExceptionHandler(WorkspaceException.class)
	public ResponseEntity<ApiResponse<?>> handleWorkspaceException(WorkspaceException exception) {
		log.error("Workspace Related Exception: ", exception);

		return ResponseEntity.status(exception.getHttpStatus())
			.body(ApiResponse.fail(exception.getHttpStatus(), exception.getMessage(), null));
	}

	@ExceptionHandler(WorkspaceMemberException.class)
	public ResponseEntity<ApiResponse<?>> handleWorkspaceMemberException(WorkspaceMemberException exception) {
		log.error("WorkspaceMember Related Exception: ", exception);

		return ResponseEntity.status(exception.getHttpStatus())
			.body(ApiResponse.fail(exception.getHttpStatus(), exception.getMessage(), null));
	}

	@ExceptionHandler(InvitationException.class)
	public ResponseEntity<ApiResponse<?>> handleInvitationException(InvitationException exception) {
		log.error("Invitation Related Exception: ", exception);

		return ResponseEntity.status(exception.getHttpStatus())
			.body(ApiResponse.fail(exception.getHttpStatus(), exception.getMessage(), null));
	}

	@ExceptionHandler(AuthorizationException.class)
	public ResponseEntity<ApiResponse<?>> handleAuthorizationException(AuthorizationException exception) {
		log.error("Authorization Related Exception: ", exception);

		return ResponseEntity.status(exception.getHttpStatus())
			.body(ApiResponse.fail(exception.getHttpStatus(), exception.getMessage(), null));
	}

	/*
	 * Todo: NotFound와 관련된 예외는 ResourceNotFoundException을 상속받아서 처리하도록 한다?
	 *  - 기존에는 도메인 별 상위 예외를 만들어서 처리했는데, 응답 상태 헤더의 섬세한 조작이 어렵다
	 *  - 해결 1: 도메인 별 상위 예외 말고, 응답 상태에 따른 상위 예외를 만들어서 처리
	 *  - 해결 2: @ResponseStatus 대신 ResponseEntity 사용
	 *  - 일단 도메인 상위 예외를 상속 받아서 사용하고, 이후 ResponseEntity를 사용하도록 리팩토링 해보자
	 */
}

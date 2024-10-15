package com.uranus.taskmanager.api.global;

import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
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
				Objects.toString(error.getRejectedValue(), ""),
				error.getDefaultMessage()
			))
			.toList();

		/*
		 * Todo: 아래 방법을 고려
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

	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ExceptionHandler(MemberException.class)
	public ApiResponse<?> handleMemberException(MemberException exception) {
		log.error("Member Related Exception: ", exception);

		return ApiResponse.fail(exception.getHttpStatus(),
			exception.getMessage(),
			null);
	}

	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ExceptionHandler(WorkspaceException.class)
	public ApiResponse<?> handleWorkspaceException(WorkspaceException exception) {
		log.error("Workspace Related Exception: ", exception);

		return ApiResponse.fail(exception.getHttpStatus(),
			exception.getMessage(),
			null);
	}

	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ExceptionHandler(WorkspaceMemberException.class)
	public ApiResponse<?> handleWorkspaceMemberException(WorkspaceMemberException exception) {
		log.error("WorkspaceMember Related Exception: ", exception);

		return ApiResponse.fail(exception.getHttpStatus(),
			exception.getMessage(),
			null);
	}

	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ExceptionHandler(InvitationException.class)
	public ApiResponse<?> handleInvitationException(InvitationException exception) {
		log.error("Invitation Related Exception: ", exception);

		return ApiResponse.fail(exception.getHttpStatus(),
			exception.getMessage(),
			null);
	}

	@ResponseStatus(HttpStatus.FORBIDDEN)
	@ExceptionHandler(AuthorizationException.class)
	public ApiResponse<?> handleAuthorizationException(AuthorizationException exception) {
		log.error("Authorization Related Exception: ", exception);

		return ApiResponse.fail(exception.getHttpStatus(),
			exception.getMessage(),
			null);
	}

	/*
	 * Todo: NotFound와 관련된 예외는 ResourceNotFoundException을 상속받아서 처리하도록 한다?
	 *  - 기존에는 도메인 별 상위 예외를 만들어서 처리했는데, 응답 상태 헤더의 섬세한 조작이 어렵다
	 *  - 해결 1: 도메인 별 상위 예외 말고, 응답 상태에 따른 상위 예외를 만들어서 처리
	 *  - 해결 2: @ResponseStatus 대신 ResponseEntity 사용
	 *  일단 도메인 상위 예외를 상속 받아서 사용하고, 이후 ResponseEntity를 사용하도록 리팩토링 해보자
	 */
}

package com.tissue.api.global;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.tissue.api.common.exception.TissueException;
import com.tissue.api.common.exception.base.AuthenticationException;
import com.tissue.api.common.exception.base.ForbiddenException;
import com.tissue.api.common.exception.base.InternalServerException;
import com.tissue.api.common.exception.base.ResourceNotFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

	private static final Pattern SENSITIVE_PATTERN = Pattern.compile(
		".*(?:password|passwd|pwd|token|secret|credential|apikey|privatekey).*",
		Pattern.CASE_INSENSITIVE
	);

	@ExceptionHandler(Exception.class)
	public ProblemDetail handleUnexpectedException(
		Exception ex,
		HttpServletRequest request
	) {
		log.error(
			"Unexpected error | method={} | path={} | error={}",
			request.getMethod(),
			request.getRequestURI(),
			ex.getMessage(),
			ex
		);

		ProblemDetail problem = ProblemDetail.forStatusAndDetail(
			HttpStatus.INTERNAL_SERVER_ERROR,
			"An unexpected error occurred"
		);

		// problem.setType(URI.create("/errors/internal_server_error"));
		problem.setTitle("INTERNAL_SERVER_ERROR");
		problem.setProperty("occurredAt", Instant.now());

		return problem;
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ProblemDetail handleNotFoundException(
		ResourceNotFoundException ex,
		HttpServletRequest request
	) {
		log.debug(
			"[{}] {} | method={} | path={}",
			ex.getErrorCode(),
			ex.getLoggingMessage(),
			request.getMethod(),
			request.getRequestURI()
		);

		return createProblemDetail(ex);
	}

	@ExceptionHandler(InternalServerException.class)
	public ProblemDetail handleInternalServerException(
		InternalServerException ex,
		HttpServletRequest request
	) {
		log.error(
			"[{}] {} | method={} | path={}",
			ex.getErrorCode(),
			ex.getLoggingMessage(),
			request.getMethod(),
			request.getRequestURI(),
			ex
		);
		// TODO: 알림 발송 (Slack, Email 등)
		// alertService.sendAlert(ex);

		return createProblemDetail(ex);
	}

	@ExceptionHandler(TissueException.class)
	public ProblemDetail handleTissueException(
		TissueException ex,
		HttpServletRequest request
	) {
		// TODO: 이 경우에는 ex를 로깅할 필요 없나?
		log.info(
			"[{}] {} | method={} | path={}",
			ex.getErrorCode(),
			ex.getLoggingMessage(),
			request.getMethod(),
			request.getRequestURI()
		);

		return createProblemDetail(ex);
	}

	// TODO: 스프링 시큐리티 쪽도 비슷하게 처리 필요
	@ExceptionHandler({AuthenticationException.class, ForbiddenException.class})
	public ProblemDetail handleSecurityException(
		TissueException ex,
		HttpServletRequest request
	) {
		log.warn(
			"[SECURITY] [{}] {} | method={} | path={} | ip={}",
			ex.getErrorCode(),
			ex.getLoggingMessage(),
			request.getMethod(),
			request.getRequestURI(),
			request.getRemoteAddr()
		);

		// TODO: 보안 감사 로그
		// securityAuditLogger.log(ex, request);

		return createProblemDetail(ex);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail handleMethodArgumentNotValid(
		MethodArgumentNotValidException ex,
		HttpServletRequest request
	) {
		Map<String, String> errors = new HashMap<>();
		ex.getBindingResult().getAllErrors().forEach(error -> {
			String fieldName = ((FieldError)error).getField();
			String errorMessage = error.getDefaultMessage();
			errors.put(fieldName, errorMessage);
		});

		log.info(
			"Validation failed | method={} | path={} | errors={}",
			request.getMethod(),
			request.getRequestURI(),
			errors
		);

		ProblemDetail problem = ProblemDetail.forStatusAndDetail(
			HttpStatus.BAD_REQUEST,
			"Validation failed for one or more fields"
		);

		// problem.setType(URI.create("/errors/validation_failed"));
		problem.setTitle("VALIDATION_FAILED");
		problem.setProperty("occurredAt", Instant.now());
		problem.setProperty("errors", errors);

		return problem;
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ProblemDetail handleConstraintViolation(
		ConstraintViolationException ex,
		HttpServletRequest request
	) {
		Map<String, String> errors = new HashMap<>();

		ex.getConstraintViolations().forEach(violation -> {
			String path = violation.getPropertyPath().toString();
			String fieldName = path.substring(path.lastIndexOf('.') + 1);
			String message = violation.getMessage();

			errors.put(fieldName, message);
		});

		log.info(
			"Request parameter validation failed | method={} | path={} | errors={}",
			request.getMethod(),
			request.getRequestURI(),
			errors
		);

		ProblemDetail problem = ProblemDetail.forStatusAndDetail(
			HttpStatus.BAD_REQUEST,
			"Request parameter validation failed"
		);

		problem.setTitle("PARAMETER_VALIDATION_FAILED");
		problem.setProperty("occurredAt", Instant.now());
		problem.setProperty("errors", errors);

		return problem;
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ProblemDetail handleHttpMessageNotReadable(
		HttpMessageNotReadableException ex,
		HttpServletRequest request
	) {
		log.warn(
			"Malformed request body | method={} | path={} | error={}",
			request.getMethod(),
			request.getRequestURI(),
			ex.getMessage()
		);

		ProblemDetail problem = ProblemDetail.forStatusAndDetail(
			HttpStatus.BAD_REQUEST,
			"Malformed JSON request"
		);

		// problem.setType(URI.create("/errors/malformed_json"));
		problem.setTitle("MALFORMED_JSON");
		problem.setProperty("occurredAt", Instant.now());

		return problem;
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ProblemDetail handleMissingServletRequestParameter(
		MissingServletRequestParameterException ex,
		HttpServletRequest request
	) {
		log.info(
			"Missing request parameter | method={} | path={} | parameter={} | type={}",
			request.getMethod(),
			request.getRequestURI(),
			ex.getParameterName(),
			ex.getParameterType()
		);

		ProblemDetail problem = ProblemDetail.forStatusAndDetail(
			HttpStatus.BAD_REQUEST,
			"Required parameter '" + ex.getParameterName() + "' is missing"
		);

		// problem.setType(URI.create("/errors/missing_request_parameter"));
		problem.setTitle("MISSING_REQUEST_PARAMETER");
		problem.setProperty("occurredAt", Instant.now());
		problem.setProperty("parameterName", ex.getParameterName());
		problem.setProperty("parameterType", ex.getParameterType());

		return problem;
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ProblemDetail handleMethodArgumentTypeMismatch(
		MethodArgumentTypeMismatchException ex,
		HttpServletRequest request
	) {
		String requiredType = ex.getRequiredType() != null
			? ex.getRequiredType().getSimpleName()
			: "unknown";

		log.info(
			"Argument type mismatch | method={} | path={} | parameter={} | value={} | requiredType={}",
			request.getMethod(),
			request.getRequestURI(),
			ex.getName(),
			ex.getValue(),
			requiredType
		);

		ProblemDetail problem = ProblemDetail.forStatusAndDetail(
			HttpStatus.BAD_REQUEST,
			"Parameter '" + ex.getName() + "' has invalid type"
		);

		// problem.setType(URI.create("/errors/argument_type_mismatch"));
		problem.setTitle("ARGUMENT_TYPE_MISMATCH");
		problem.setProperty("occurredAt", Instant.now());
		problem.setProperty("parameterName", ex.getName());
		problem.setProperty("providedValue", ex.getValue());
		problem.setProperty("requiredType", requiredType);

		return problem;
	}

	@ExceptionHandler(OptimisticLockingFailureException.class)
	public ProblemDetail handleOptimisticLockingFailure(
		OptimisticLockingFailureException ex,
		HttpServletRequest request
	) {
		log.warn(
			"Optimistic lock failed | method={} | path={} | error={}",
			request.getMethod(),
			request.getRequestURI(),
			ex.getMessage()
		);

		ProblemDetail problem = ProblemDetail.forStatusAndDetail(
			HttpStatus.CONFLICT,
			"The resource was modified by another user. Please refresh and try again."
		);

		// problem.setType(URI.create("/errors/optimistic_lock_failed"));
		problem.setTitle("OPTIMISTIC_LOCK_FAILED");
		problem.setProperty("occurredAt", Instant.now());

		return problem;
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ProblemDetail handleDataIntegrityViolation(
		DataIntegrityViolationException ex,
		HttpServletRequest request
	) {
		log.warn(
			"Data integrity violation | method={} | path={} | error={}",
			request.getMethod(),
			request.getRequestURI(),
			ex.getMessage()
		);

		// 구체적인 에러 파싱
		String detail = "A database constraint was violated";
		if (ex.getMessage().contains("duplicate key")) {
			detail = "Duplicate entry detected";
		} else if (ex.getMessage().contains("foreign key")) {
			detail = "Referenced resource does not exist";
		}

		ProblemDetail problem = ProblemDetail.forStatusAndDetail(
			HttpStatus.CONFLICT,
			detail
		);

		// problem.setType(URI.create("/errors/data_integrity_violation"));
		problem.setTitle("DATA_INTEGRITY_VIOLATION");
		problem.setProperty("occurredAt", Instant.now());

		return problem;
	}

	private ProblemDetail createProblemDetail(TissueException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(
			ex.getHttpStatus(),
			ex.getMessage()
		);

		// TODO: API 문서화 이후 추가
		// problem.setType(URI.create("/errors/" + ex.getErrorCode().toLowerCase()));
		problem.setTitle(ex.getErrorCode());
		problem.setProperty("occurredAt", Instant.now());

		ex.getContext().forEach((key, value) -> {
			if (isSafeToExpose(key)) {
				problem.setProperty(key, value);
			}
		});

		return problem;
	}

	private boolean isSafeToExpose(String key) {
		if (key == null || key.isEmpty()) {
			return false;
		}
		return !SENSITIVE_PATTERN.matcher(key).matches();
	}
}

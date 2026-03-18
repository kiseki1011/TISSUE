package com.tissue.global;

import com.tissue.shared.exception.TissueException;
import com.tissue.shared.exception.base.ForbiddenException;
import com.tissue.shared.exception.base.InternalServerException;
import com.tissue.shared.exception.base.RateLimitExceededException;
import com.tissue.shared.exception.base.ResourceNotFoundException;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

// TODO: Use problem.setType() after API documentation is finished
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Pattern SENSITIVE_PATTERN = Pattern.compile(
            ".*(?:password|passwd|pwd|token|secret|credential|apikey|privatekey).*", Pattern.CASE_INSENSITIVE);

    @ExceptionHandler(ForbiddenException.class)
    public ProblemDetail handleSecurityException(ForbiddenException ex) {
        log.warn("{}: {}", ex.getErrorCode().name(), ex.getLoggingMessage());

        return createProblemDetail(ex);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentialsException(BadCredentialsException ex) {
        log.info("Invalid credentials provided");

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        problem.setTitle("LOGIN_FAILED");
        problem.setProperty("occurredAt", Instant.now());

        return problem;
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Authentication failed");
        problem.setTitle("AUTHENTICATION_FAILED");
        problem.setProperty("occurredAt", Instant.now());

        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");

        // problem.setType(URI.create("/errors/internal-server-error"));
        problem.setTitle("UNEXPECTED_ERROR");
        problem.setProperty("occurredAt", Instant.now());

        return problem;
    }

    @ExceptionHandler(InternalServerException.class)
    public ProblemDetail handleInternalServerException(InternalServerException ex) {
        log.error("{}: {}", ex.getErrorCode().name(), ex.getLoggingMessage(), ex);
        return createProblemDetail(ex);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFoundException(ResourceNotFoundException ex) {
        log.debug("{}: {}", ex.getErrorCode().name(), ex.getLoggingMessage());
        return createProblemDetail(ex);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ProblemDetail handleRateLimitExceededException(RateLimitExceededException ex) {
        log.warn("{}: {}", ex.getErrorCode().name(), ex.getLoggingMessage());
        return createProblemDetail(ex);
    }

    @ExceptionHandler(TissueException.class)
    public ProblemDetail handleTissueException(TissueException ex) {
        log.info("{}: {}", ex.getErrorCode().name(), ex.getLoggingMessage());

        return createProblemDetail(ex);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = (error instanceof FieldError fe) ? fe.getField() : error.getObjectName();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.info("Validation failed: errors={}", errors);

        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed for one or more fields");

        // problem.setType(URI.create("/errors/validation-failed"));
        problem.setTitle("VALIDATION_FAILED");
        problem.setProperty("occurredAt", Instant.now());
        problem.setProperty("errors", errors);

        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getConstraintViolations().forEach(violation -> {
            String path = violation.getPropertyPath().toString();
            String fieldName = path.substring(path.lastIndexOf('.') + 1);
            String message = violation.getMessage();

            errors.put(fieldName, message);
        });

        log.info("Request parameter validation failed | errors={}", errors);

        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request parameter validation failed");

        problem.setTitle("PARAMETER_VALIDATION_FAILED");
        problem.setProperty("occurredAt", Instant.now());
        problem.setProperty("errors", errors);

        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Malformed JSON request: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Malformed JSON request");

        // problem.setType(URI.create("/errors/malformed-json"));
        problem.setTitle("MALFORMED_JSON");
        problem.setProperty("occurredAt", Instant.now());

        return problem;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingServletRequestParameter(MissingServletRequestParameterException ex) {

        log.info("Required parameter '{}' is missing | type={}", ex.getParameterName(), ex.getParameterType());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Required parameter '" + ex.getParameterName() + "' is missing");

        // problem.setType(URI.create("/errors/missing-request-parameter"));
        problem.setTitle("MISSING_REQUEST_PARAMETER");
        problem.setProperty("occurredAt", Instant.now());
        problem.setProperty("parameterName", ex.getParameterName());
        problem.setProperty("parameterType", ex.getParameterType());

        return problem;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String requiredType =
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";

        log.info(
                "Parameter '{}' has invalid type | value={} | requiredType={}",
                ex.getName(),
                ex.getValue(),
                requiredType);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Parameter '" + ex.getName() + "' has invalid type");

        // problem.setType(URI.create("/errors/argument-type-mismatch"));
        problem.setTitle("ARGUMENT_TYPE_MISMATCH");
        problem.setProperty("occurredAt", Instant.now());
        problem.setProperty("parameterName", ex.getName());
        problem.setProperty("providedValue", ex.getValue());
        problem.setProperty("requiredType", requiredType);

        return problem;
    }

    @ExceptionHandler({OptimisticLockException.class, OptimisticLockingFailureException.class})
    public ProblemDetail handleOptimisticLockingFailure(Exception ex) {
        log.warn("The resource was already modified | error={}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "The resource was modified by another user. Please refresh and try again.");

        // problem.setType(URI.create("/errors/optimistic-lock-failed"));
        problem.setTitle("OPTIMISTIC_LOCK_FAILED");
        problem.setProperty("occurredAt", Instant.now());

        return problem;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String detail = "A database constraint was violated";

        log.warn("Database constraint violated | error={}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, detail);

        // problem.setType(URI.create("/errors/data-integrity-violation"));
        problem.setTitle("DATA_INTEGRITY_VIOLATION");
        problem.setProperty("occurredAt", Instant.now());

        return problem;
    }

    private ProblemDetail createProblemDetail(TissueException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getHttpStatus(), ex.getMessage());

        // problem.setType(URI.create("/errors/" + toKebabCase(ex.getErrorCode().name())));
        problem.setTitle(ex.getErrorCode().name());
        problem.setProperty("occurredAt", Instant.now());

        ex.getContext().forEach((key, value) -> {
            if (isSafeToExpose(key)) {
                problem.setProperty(key, value);
            }
        });

        return problem;
    }

    private String toKebabCase(String text) {
        return text.toLowerCase().replace('_', '-');
    }

    private boolean isSafeToExpose(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        return !SENSITIVE_PATTERN.matcher(key).matches();
    }
}

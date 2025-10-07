package com.tissue.api.common.exception.type;

import java.util.List;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.dto.FieldErrorDto;
import com.tissue.api.common.exception.TissueException;

import lombok.Getter;

@Getter
public class FieldValidationException extends TissueException {

	private final List<FieldErrorDto> fieldErrors;

	public FieldValidationException(String message, List<FieldErrorDto> fieldErrors) {
		super(message, HttpStatus.BAD_REQUEST);
		this.fieldErrors = fieldErrors;
	}

	public FieldValidationException(String message, Throwable cause, List<FieldErrorDto> fieldErrors) {
		super(message, HttpStatus.BAD_REQUEST, cause);
		this.fieldErrors = fieldErrors;
	}
}

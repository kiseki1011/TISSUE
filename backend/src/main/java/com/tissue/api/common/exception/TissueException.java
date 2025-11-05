package com.tissue.api.common.exception;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;

public abstract class TissueException extends RuntimeException {

	private final Map<String, Object> context = new HashMap<>();

	protected TissueException(String message) {
		super(message);
	}

	public abstract HttpStatus getHttpStatus();

	public String getErrorCode() {
		return this.getClass().getSimpleName()
			.replaceAll("Exception$", "")
			.replaceAll("([a-z])([A-Z])", "$1_$2")
			.toUpperCase();
	}

	public String getLoggingMessage() {
		if (context.isEmpty()) {
			return getMessage();
		}

		StringBuilder sb = new StringBuilder(getMessage());
		sb.append(" | context={");

		context.forEach((key, value) ->
			sb.append(key).append("=").append(value).append(", ")
		);

		if (!context.isEmpty()) {
			sb.setLength(sb.length() - 2);
		}
		sb.append("}");

		return sb.toString();
	}

	protected void addContext(String key, Object value) {
		this.context.put(key, value);
	}

	public Map<String, Object> getContext() {
		return Collections.unmodifiableMap(context);
	}
}

package com.tissue.api.security.authentication;

import java.util.Arrays;

import com.tissue.api.security.authentication.exception.JwtAuthenticationException;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TokenType {

	ACCESS("access"),
	REFRESH("refresh");

	private final String value;

	public static TokenType from(String value) {
		return Arrays.stream(values())
			.filter(t -> t.value.equals(value))
			.findFirst()
			.orElseThrow(() -> new JwtAuthenticationException("Invalid token type: " + value));
	}
}

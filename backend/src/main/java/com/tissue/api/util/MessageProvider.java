package com.tissue.api.util;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import lombok.RequiredArgsConstructor;

// @Component
@RequiredArgsConstructor
public class MessageProvider {

	private final MessageSource messageSource;

	public String getMessage(String code, Object... args) {
		return messageSource.getMessage(
			code,
			args,
			LocaleContextHolder.getLocale()
		);
	}
}

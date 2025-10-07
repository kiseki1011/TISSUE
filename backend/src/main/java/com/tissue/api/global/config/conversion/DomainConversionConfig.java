package com.tissue.api.global.config.conversion;

import java.time.Instant;
import java.time.OffsetDateTime;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;

// TODO: issue.base.config로 옮기기
@Configuration
public class DomainConversionConfig {

	@Bean
	@Qualifier("domainConversionService")
	public ConversionService domainConversionService() {
		ApplicationConversionService cs = new ApplicationConversionService();

		cs.addConverter(String.class, Instant.class, s -> OffsetDateTime.parse(s).toInstant());

		return cs;
	}
}

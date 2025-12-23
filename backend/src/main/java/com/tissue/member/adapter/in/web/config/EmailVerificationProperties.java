package com.tissue.member.adapter.in.web.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@Data
@ConfigurationProperties(prefix = "email.verification")
public class EmailVerificationProperties {

	private String successUrl;
	private String failureUrl;
	private String verificationUrl;
}

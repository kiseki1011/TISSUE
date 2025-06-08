package com.tissue.api.member.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "email.verification")
@Getter
@Setter
public class EmailVerificationProperties {
	private String successUrl;
	private String failureUrl;
	private String verificationUrl;
}

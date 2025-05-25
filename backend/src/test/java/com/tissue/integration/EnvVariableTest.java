package com.tissue.integration;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class EnvVariableTest {

	@Value("${GMAIL_USERNAME}")
	private String gmailUsername;

	@Value("${GMAIL_PASSWORD}")
	private String gmailPassword;

	@Test
	@DisplayName("환경변수가 정상적으로 주입되는지 확인한다")
	void checkIfGmailEnvVariableIsAvailable() {
		assertThat(gmailUsername).isNotNull();
		assertThat(gmailPassword).isNotNull();
		System.out.println("GMAIL_USERNAME = " + gmailUsername);
	}
}

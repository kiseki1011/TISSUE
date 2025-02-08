package com.tissue.api.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordEncoder {
	private final BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

	public String encode(String rawPassword) {
		if (rawPassword == null) {
			return null;
		}
		return bCryptPasswordEncoder.encode(rawPassword);
	}

	public boolean matches(String rawPassword, String encodedPassword) {
		// Todo: 패스워드가 null이면 패스워드 검증 없이 통과 가능하도록 구현할까?
		// if (encodedPassword == null) {
		// 	return true;
		// }

		return bCryptPasswordEncoder.matches(rawPassword, encodedPassword);
	}
}

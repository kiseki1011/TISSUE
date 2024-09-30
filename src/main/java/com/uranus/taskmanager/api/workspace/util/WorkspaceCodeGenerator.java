package com.uranus.taskmanager.api.workspace.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class WorkspaceCodeGenerator {

	/**
	 * Todo
	 * 상수 정리하기
	 * 해시 알고리즘 변경(보안 필요 없음), 더 빠른 알고리즘 고려
	 */
	private static final int WORKSPACE_CODE_LENGTH = 8;
	private static final String HASH_ALGORITHM = "SHA-256";

	/**
	 * UUID를 해시 함수의 입력으로 주고 Base62 인코딩한다.
	 * 해당 Base62 문자열의 앞 8자리를 truncate해서 반환한다.
	 */
	public String generateWorkspaceCode() {
		byte[] hash = hashFunction(UUID.randomUUID().toString());
		String base62Encoded = Base62Encoder.encode(hash);
		return base62Encoded.substring(0, WORKSPACE_CODE_LENGTH);
	}

	/**
	 * String을 입력으로 받아서 SHA-256 방식으로 해시값을 생성한다.
	 */
	private byte[] hashFunction(String inputString) {
		try {
			MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
			return md.digest(inputString.getBytes());
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e); // Todo: 커스텀 예외 만들기, HashAlgorithmNotFoundException
		}
	}
}

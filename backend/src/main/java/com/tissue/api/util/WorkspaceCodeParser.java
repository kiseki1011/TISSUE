package com.tissue.api.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.InvalidOperationException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class WorkspaceCodeParser {

	private static final Pattern WORKSPACE_CODE_PATTERN =
		Pattern.compile("/api/v1/workspaces/([A-Za-z0-9]{8})(?:/.*)?");

	private final Matcher matcher;

	public WorkspaceCodeParser() {
		this.matcher = WORKSPACE_CODE_PATTERN.matcher("");
	}

	/**
	 * URI에서 워크스페이스 코드를 추출한다.
	 * 워크스페이스 코드는 8자리 영숫자로 구성되어야 한다.
	 *
	 * @param uri HTTP 요청의 URI
	 * @return 추출된 워크스페이스 코드
	 * @throws InvalidOperationException URI가 올바르지 않거나 워크스페이스 코드가 패턴에 부합하지 않는 경우
	 */
	public String extractWorkspaceCode(String uri) {
		matcher.reset(uri);

		if (matcher.matches()) {
			return matcher.group(1);
		}
		throw new InvalidOperationException(String.format("Invalid workspace code in URI. URI: %s", uri));
	}
}

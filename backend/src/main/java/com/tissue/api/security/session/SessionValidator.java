package com.tissue.api.security.session;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.tissue.api.common.dto.PermissionContext;
import com.tissue.api.common.enums.PermissionType;
import com.tissue.api.security.authentication.exception.UserNotLoggedInException;
import com.tissue.api.security.authorization.exception.InvalidPermissionException;
import com.tissue.api.security.authorization.exception.PermissionMismatchException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionValidator {

	private final SessionManager sessionManager;

	public void validateLoginStatus(HttpServletRequest request) {
		Optional<HttpSession> session = Optional.ofNullable(request.getSession(false));
		if (session.isEmpty() || session.map(s -> s.getAttribute(SessionAttributes.LOGIN_MEMBER_ID)).isEmpty()) {
			throw new UserNotLoggedInException();
		}
	}

	public void validatePermissionInSession(HttpSession session, PermissionType permissionType) {

		PermissionContext permissionContext = getPermissionContext(session, permissionType);

		if (isInvalidPermission(permissionContext)) {
			sessionManager.clearPermission(session);
			throw new InvalidPermissionException();
		}
	}

	private PermissionContext getPermissionContext(HttpSession session, PermissionType permissionType) {
		PermissionType storedPermissionType = (PermissionType)session.getAttribute(SessionAttributes.PERMISSION_TYPE);
		Boolean permissionExists = (Boolean)session.getAttribute(SessionAttributes.PERMISSION_EXISTS);
		LocalDateTime expiresAt = (LocalDateTime)session.getAttribute(SessionAttributes.PERMISSION_EXPIRES_AT);

		// 현재 세션에 저장된 권한 정보가 요청한 권한 타입과 일치하는지 확인
		if (storedPermissionType != permissionType) {
			throw new PermissionMismatchException();
		}

		return new PermissionContext(storedPermissionType, permissionExists, expiresAt);
	}

	private boolean isInvalidPermission(PermissionContext permissionContext) {
		return permissionContext.permissionIsNull()
			|| permissionContext.permissionIsFalse()
			|| permissionContext.expirationDateIsNull()
			|| permissionContext.permissionExpired();
	}
}

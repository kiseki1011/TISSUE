package com.tissue.api.security.session;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.tissue.api.common.dto.PermissionContext;
import com.tissue.api.common.enums.PermissionType;
import com.tissue.api.common.exception.type.ForbiddenOperationException;
import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.common.exception.type.UnauthorizedException;

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
			throw new UnauthorizedException("Login is required to access.");
		}
	}

	public void validatePermissionInSession(HttpSession session, PermissionType permissionType) {

		PermissionContext permissionContext = getPermissionContext(session, permissionType);

		if (isInvalidPermission(permissionContext)) {
			sessionManager.clearPermission(session);
			throw new ForbiddenOperationException(
				String.format(
					"You do not have permission or the permission has expired. permissionType: %s",
					permissionType
				)
			);
		}
	}

	private PermissionContext getPermissionContext(HttpSession session, PermissionType permissionType) {
		PermissionType storedPermissionType = (PermissionType)session.getAttribute(SessionAttributes.PERMISSION_TYPE);
		Boolean permissionExists = (Boolean)session.getAttribute(SessionAttributes.PERMISSION_EXISTS);
		LocalDateTime expiresAt = (LocalDateTime)session.getAttribute(SessionAttributes.PERMISSION_EXPIRES_AT);

		if (storedPermissionType != permissionType) {
			throw new InvalidOperationException(
				String.format(
					"Permission type of request does not match with current permission type."
						+ " Requested permission: %s, Current permission: %s",
					permissionType, storedPermissionType
				)
			);
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

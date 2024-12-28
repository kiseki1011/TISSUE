package com.tissue.api.security.session;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.tissue.api.common.dto.PermissionContext;
import com.tissue.api.common.enums.PermissionType;
import com.tissue.api.security.authentication.exception.UserNotLoggedInException;
import com.tissue.api.security.authorization.exception.UpdatePermissionException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class SessionValidator {

	public void validateLoginStatus(HttpServletRequest request) {
		Optional<HttpSession> session = Optional.ofNullable(request.getSession(false));
		if (session.isEmpty() || session.map(s -> s.getAttribute(SessionAttributes.LOGIN_MEMBER_ID)).isEmpty()) {
			throw new UserNotLoggedInException();
		}
	}

	public void validateMemberPermissionInSession(HttpSession session, PermissionType permissionType) {

		PermissionContext permissionContext = getPermissionContext(session, permissionType);

		if (isInvalidPermission(permissionContext)
		) {
			clearPermission(session, permissionType);
			throw new UpdatePermissionException();
		}
	}

	private PermissionContext getPermissionContext(HttpSession session, PermissionType permissionType) {
		return switch (permissionType) {
			case UPDATE -> new PermissionContext(
				(Boolean)session.getAttribute(SessionAttributes.MEMBER_UPDATE_AUTH),
				(LocalDateTime)session.getAttribute(SessionAttributes.MEMBER_UPDATE_AUTH_EXPIRES_AT)
			);
			case DELETE -> new PermissionContext(
				(Boolean)session.getAttribute(SessionAttributes.MEMBER_DELETE_AUTH),
				(LocalDateTime)session.getAttribute(SessionAttributes.MEMBER_DELETE_AUTH_EXPIRES_AT)
			);
			default -> throw new IllegalArgumentException("Unknown permission type: " + permissionType);
		};
	}

	private boolean isInvalidPermission(PermissionContext permissionContext) {
		return updatePermissionIsNull(permissionContext.updatePermission())
			|| updatePermissionIsFalse(permissionContext.updatePermission())
			|| expirationPeriodIsNull(permissionContext.expiresAt())
			|| LocalDateTime.now().isAfter(permissionContext.expiresAt());
	}

	private void clearPermission(HttpSession session, PermissionType permissionType) {
		switch (permissionType) {
			case UPDATE -> {
				session.removeAttribute(SessionAttributes.MEMBER_UPDATE_AUTH);
				session.removeAttribute(SessionAttributes.MEMBER_UPDATE_AUTH_EXPIRES_AT);
				log.info("Update permission cleared due to validation failure.");
			}
			case DELETE -> {
				session.removeAttribute(SessionAttributes.MEMBER_DELETE_AUTH);
				session.removeAttribute(SessionAttributes.MEMBER_DELETE_AUTH_EXPIRES_AT);
				log.info("Delete permission cleared due to validation failure.");
			}
			default -> throw new IllegalArgumentException("Unknown permission type: " + permissionType);
		}
	}

	private void clearUpdatePermission(HttpSession session) {
		session.removeAttribute(SessionAttributes.MEMBER_UPDATE_AUTH);
		session.removeAttribute(SessionAttributes.MEMBER_UPDATE_AUTH_EXPIRES_AT);
		log.info("Update permission cleared due to validation failure.");
	}

	private void clearDeletePermission(HttpSession session) {
		session.removeAttribute(SessionAttributes.MEMBER_DELETE_AUTH);
		session.removeAttribute(SessionAttributes.MEMBER_DELETE_AUTH_EXPIRES_AT);
		log.info("Delete permission cleared due to validation failure.");
	}

	private boolean expirationPeriodIsNull(LocalDateTime expiresAt) {
		return expiresAt == null;
	}

	private boolean updatePermissionIsNull(Boolean updatePermission) {
		return updatePermission == null;
	}

	private boolean updatePermissionIsFalse(Boolean updatePermission) {
		return !updatePermission;
	}
}

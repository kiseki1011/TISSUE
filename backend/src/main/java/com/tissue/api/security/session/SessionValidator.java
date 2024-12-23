package com.tissue.api.security.session;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Component;

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

	public void validateUpdatePermission(HttpSession session) {
		Boolean updatePermission = (Boolean)session.getAttribute(SessionAttributes.UPDATE_AUTH);
		LocalDateTime expiresAt = (LocalDateTime)session.getAttribute(SessionAttributes.UPDATE_AUTH_EXPIRES_AT);

		if (updatePermissionIsNull(updatePermission)
			|| updatePermissionIsFalse(updatePermission)
			|| expirationPeriodIsNull(expiresAt)
			|| LocalDateTime.now().isAfter(expiresAt)
		) {
			clearUpdatePermission(session);
			throw new UpdatePermissionException();
		}
	}

	private void clearUpdatePermission(HttpSession session) {
		session.removeAttribute(SessionAttributes.UPDATE_AUTH);
		session.removeAttribute(SessionAttributes.UPDATE_AUTH_EXPIRES_AT);
		log.info("Update permission cleared due to validation failure.");
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

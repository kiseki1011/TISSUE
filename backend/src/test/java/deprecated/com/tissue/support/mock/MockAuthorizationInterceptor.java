package deprecated.com.tissue.support.mock;

import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class MockAuthorizationInterceptor implements HandlerInterceptor {
	private final boolean hasSufficientRole;

	public MockAuthorizationInterceptor(boolean hasSufficientRole) {
		this.hasSufficientRole = hasSufficientRole;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (hasSufficientRoleIsFalse()) {
			// TODO: InsufficientWorkspaceRoleException, 그런데 어차피 이 클래스 제거하거나 수정할 예정
			throw new RuntimeException("[MockAuthorizationInterceptor] insufficient workspace role");
		}
		return true;
	}

	private boolean hasSufficientRoleIsFalse() {
		return !hasSufficientRole;
	}
}
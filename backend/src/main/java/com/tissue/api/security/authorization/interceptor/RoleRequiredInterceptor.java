package com.tissue.api.security.authorization.interceptor;

import java.util.Map;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import com.tissue.api.common.exception.type.AuthenticationFailedException;
import com.tissue.api.common.exception.type.ForbiddenOperationException;
import com.tissue.api.common.exception.type.InvalidRequestException;
import com.tissue.api.security.authentication.MemberUserDetails;
import com.tissue.api.workspacemember.application.service.command.WorkspaceMemberReader;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoleRequiredInterceptor implements HandlerInterceptor {

	public static final String PATH_VAR_WORKSPACE_CODE = "workspaceCode";
	private final WorkspaceMemberReader workspaceMemberReader;

	private static boolean isNotHandlerMethod(Object handler) {
		return !(handler instanceof HandlerMethod);
	}

	@Override
	public boolean preHandle(
		HttpServletRequest request,
		HttpServletResponse response,
		Object handler
	) {
		if (isNotHandlerMethod(handler)) {
			return true;
		}

		RoleRequired roleRequired = getRoleRequired(handler)
			.orElse(null);

		if (roleRequired == null) {
			return true;
		}

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new AuthenticationFailedException("Authentication required.");
		}

		MemberUserDetails userDetails = (MemberUserDetails)authentication.getPrincipal();
		Long loginMemberId = userDetails.getMemberId();

		// extract workspaceCode from path variable
		String workspaceCode = getPathVariable(request, PATH_VAR_WORKSPACE_CODE);

		// TODO: cache WorkspaceMember later
		WorkspaceMember workspaceMember = workspaceMemberReader.findWorkspaceMember(loginMemberId, workspaceCode);

		// check if workspaceMember has the minimum required role
		log.debug("Validating workspace role of workspace member. memberId: {}, workspaceCode: {}",
			loginMemberId, workspaceCode);

		validateRole(workspaceMember, roleRequired);

		return true;
	}

	private Optional<RoleRequired> getRoleRequired(Object handler) {
		HandlerMethod handlerMethod = (HandlerMethod)handler;
		return Optional.ofNullable(handlerMethod.getMethodAnnotation(RoleRequired.class));
	}

	// TODO: consider separating to util class
	private void validateRole(
		WorkspaceMember workspaceMember,
		RoleRequired roleRequired
	) {
		boolean isLowerThanRequiredRole = workspaceMember.getRole().isLowerThan(roleRequired.role());

		if (isLowerThanRequiredRole) {
			throw new ForbiddenOperationException(String.format("Workspace role must be at least %s. Current role: %s",
				roleRequired.role(), workspaceMember.getRole()));
		}
	}

	// TODO: consider separating to util class
	private String getPathVariable(HttpServletRequest request, String name) {
		Object attr = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

		if (!(attr instanceof Map<?, ?> rawMap)) {
			throw new InvalidRequestException("Path variables are not available in this request.");
		}

		@SuppressWarnings("unchecked")
		Map<String, String> pathVars = (Map<String, String>)rawMap;

		return Optional.ofNullable(pathVars.get(name))
			.orElseThrow(() -> new InvalidRequestException("{" + name + "} path variable is required."));
	}
}

package com.tissue.api.notification.presentation.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.notification.application.service.command.NotificationPreferenceService;
import com.tissue.api.notification.presentation.dto.request.UpdateNotificationPreferenceRequest;
import com.tissue.api.security.authentication.interceptor.LoginRequired;
import com.tissue.api.security.authentication.resolver.ResolveLoginMember;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.workspacemember.domain.model.enums.WorkspaceRole;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces/{workspaceCode}/notifications/preferences")
public class NotificationPreferenceController {

	private final NotificationPreferenceService preferenceService;

	@LoginRequired
	@RoleRequired(role = WorkspaceRole.VIEWER)
	@PostMapping
	public ApiResponse<Void> updatePreferences(
		@PathVariable String workspaceCode,
		@ResolveLoginMember Long loginMemberId,
		@RequestBody UpdateNotificationPreferenceRequest request
	) {
		preferenceService.updatePreference(workspaceCode, loginMemberId, request);
		return ApiResponse.okWithNoContent("Updated notification preference.");
	}
}

package com.tissue.api.notification.presentation.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.notification.application.service.command.NotificationPreferenceService;
import com.tissue.api.notification.presentation.dto.request.UpdateNotificationPreferenceRequest;
import com.tissue.api.security.authentication.MemberUserDetails;
import com.tissue.api.security.authentication.resolver.CurrentMember;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.workspacemember.domain.model.enums.WorkspaceRole;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces/{workspaceCode}/notifications/preferences")
public class NotificationPreferenceController {

	private final NotificationPreferenceService preferenceService;

	@RoleRequired(role = WorkspaceRole.VIEWER)
	@PostMapping
	public ApiResponse<Void> updatePreferences(
		@PathVariable String workspaceCode,
		@CurrentMember MemberUserDetails userDetails,
		@RequestBody UpdateNotificationPreferenceRequest request
	) {
		preferenceService.updatePreference(workspaceCode, userDetails.getMemberId(), request);
		return ApiResponse.okWithNoContent("Updated notification preference.");
	}
}

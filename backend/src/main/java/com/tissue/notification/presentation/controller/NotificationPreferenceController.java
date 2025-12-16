package com.tissue.notification.presentation.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.common.dto.ApiResponse;
import com.tissue.notification.application.service.command.NotificationPreferenceService;
import com.tissue.notification.presentation.dto.request.UpdateNotificationPreferenceRequest;
import com.tissue.security.authentication.MemberUserDetails;
import com.tissue.security.authentication.resolver.CurrentMember;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces/{workspaceKey}/notifications/preferences")
public class NotificationPreferenceController {

	private final NotificationPreferenceService preferenceService;

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

package com.tissue.notification.presentation.controller;

import com.tissue.notification.application.service.command.NotificationPreferenceService;
import com.tissue.notification.presentation.dto.request.UpdateNotificationPreferenceRequest;
import com.tissue.security.authentication.domain.MemberDetails;
import com.tissue.security.authentication.presentation.annotation.CurrentMember;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces/{workspaceKey}/notifications/preferences")
public class NotificationPreferenceController {

    private final NotificationPreferenceService preferenceService;

    @PostMapping
    public ResponseEntity<Void> updatePreferences(
            @PathVariable String workspaceCode,
            @CurrentMember MemberDetails userDetails,
            @RequestBody UpdateNotificationPreferenceRequest request) {
        preferenceService.updatePreference(workspaceCode, userDetails.getMemberId(), request);
        return ResponseEntity.noContent().build();
    }
}

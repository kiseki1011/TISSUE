package com.tissue.notification.adapter.web;

import com.tissue.global.security.principal.MemberDetails;
import com.tissue.notification.adapter.web.request.UpdateNotificationPreferenceRequest;
import com.tissue.notification.application.dto.response.NotificationPreferenceResponse;
import com.tissue.notification.application.service.NotificationPreferenceService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping
    public ResponseEntity<List<NotificationPreferenceResponse>> getPreferences(
        @PathVariable String workspaceKey, @AuthenticationPrincipal MemberDetails currentMember) {

        List<NotificationPreferenceResponse> responses =
            preferenceService.getPreferences(workspaceKey, currentMember.getMemberId());
        return ResponseEntity.ok(responses);
    }

    @PostMapping
    public ResponseEntity<Void> updatePreferences(
        @PathVariable String workspaceKey,
        @RequestBody UpdateNotificationPreferenceRequest request,
        @AuthenticationPrincipal MemberDetails currentMember) {

        preferenceService.updatePreference(workspaceKey, currentMember.getMemberId(), request);
        return ResponseEntity.noContent().build();
    }
}

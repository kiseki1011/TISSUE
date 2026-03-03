package com.tissue.feature.notification.web;

import com.tissue.feature.notification.application.dto.response.NotificationPreferenceResponse;
import com.tissue.feature.notification.application.service.NotificationPreferenceService;
import com.tissue.feature.notification.web.request.UpdateNotificationPreferenceRequest;
import com.tissue.principal.CurrentMember;
import com.tissue.principal.MemberDetails;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
            @PathVariable String workspaceKey, @CurrentMember MemberDetails currentMember) {

        List<NotificationPreferenceResponse> responses =
                preferenceService.getPreferences(workspaceKey, currentMember.getMemberId());
        return ResponseEntity.ok(responses);
    }

    @PostMapping
    public ResponseEntity<Void> updatePreferences(
            @PathVariable String workspaceKey,
            @RequestBody UpdateNotificationPreferenceRequest request,
            @CurrentMember MemberDetails currentMember) {

        preferenceService.updatePreference(workspaceKey, request.toCommand(), currentMember.getMemberId());
        return ResponseEntity.noContent().build();
    }
}

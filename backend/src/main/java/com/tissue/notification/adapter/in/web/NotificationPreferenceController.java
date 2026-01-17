package com.tissue.notification.adapter.in.web;

import com.tissue.notification.adapter.in.web.dto.request.UpdateNotificationPreferenceRequest;
import com.tissue.notification.application.dto.response.NotificationPreferenceResponse;
import com.tissue.notification.application.service.NotificationPreferenceService;
import com.tissue.security.authentication.domain.MemberDetails;
import com.tissue.security.authentication.presentation.annotation.CurrentMember;
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
            @PathVariable String workspaceKey, @CurrentMember MemberDetails userDetails) {

        List<NotificationPreferenceResponse> responses =
                preferenceService.getPreferences(workspaceKey, userDetails.getMemberId());
        return ResponseEntity.ok(responses);
    }

    @PostMapping
    public ResponseEntity<Void> updatePreferences(
            @PathVariable String workspaceKey,
            @CurrentMember MemberDetails userDetails,
            @RequestBody UpdateNotificationPreferenceRequest request) {

        preferenceService.updatePreference(workspaceKey, userDetails.getMemberId(), request);
        return ResponseEntity.noContent().build();
    }
}

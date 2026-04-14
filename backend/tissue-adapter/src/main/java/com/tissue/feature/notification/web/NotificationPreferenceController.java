package com.tissue.feature.notification.web;

import com.tissue.feature.notification.application.dto.response.NotificationPreferenceResponse;
import com.tissue.feature.notification.application.service.NotificationPreferenceService;
import com.tissue.feature.notification.web.request.UpdateNotificationPreferenceRequest;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notification Preference")
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/notifications/preferences")
@RequiredArgsConstructor
public class NotificationPreferenceController {

    private final NotificationPreferenceService preferenceService;

    @Operation(
            summary = "Get notification preferences",
            description = "Retrieve the current user's notification preferences.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Preferences retrieved"),
        @ApiResponse(responseCode = "404", description = "Workspace not found", content = @Content)
    })
    @GetMapping
    public ResponseEntity<List<NotificationPreferenceResponse>> getPreferences(
            @PathVariable String workspaceKey, @CurrentMember MemberDetails currentMember) {
        List<NotificationPreferenceResponse> responses =
                preferenceService.getPreferences(workspaceKey, currentMember.getMemberId());

        return ResponseEntity.ok(responses);
    }

    @Operation(
            summary = "Update notification preferences",
            description = "Update the current user's notification preferences.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Preferences updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Workspace not found", content = @Content)
    })
    @PostMapping
    public ResponseEntity<Void> updatePreferences(
            @PathVariable String workspaceKey,
            @RequestBody @Valid UpdateNotificationPreferenceRequest request,
            @CurrentMember MemberDetails currentMember) {
        preferenceService.updatePreference(workspaceKey, request.toCommand(), currentMember.getMemberId());

        return ResponseEntity.noContent().build();
    }
}

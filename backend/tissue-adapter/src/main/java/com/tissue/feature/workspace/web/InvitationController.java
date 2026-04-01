package com.tissue.feature.workspace.web;

import com.tissue.feature.workspace.application.dto.response.query.InvitationDetail;
import com.tissue.feature.workspace.application.port.usecase.InvitationUseCase;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Invitation")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/invitations")
public class InvitationController {

    private final InvitationUseCase invitationUseCase;

    @Operation(summary = "Accept invitation", description = "Accept a workspace invitation.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Invitation accepted"),
        @ApiResponse(responseCode = "404", description = "Invitation not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Invitation already processed", content = @Content)
    })
    @PostMapping("/{invitationId}/accept")
    public ResponseEntity<Void> accept(@PathVariable Long invitationId, @CurrentMember MemberDetails memberDetails) {
        invitationUseCase.accept(memberDetails.getMemberId(), invitationId);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reject invitation", description = "Reject a workspace invitation.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Invitation rejected"),
        @ApiResponse(responseCode = "404", description = "Invitation not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Invitation already processed", content = @Content)
    })
    @PostMapping("/{invitationId}/reject")
    public ResponseEntity<Void> reject(@PathVariable Long invitationId, @CurrentMember MemberDetails memberDetails) {
        invitationUseCase.reject(memberDetails.getMemberId(), invitationId);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "List my invitations", description = "Retrieve all pending invitations for the current user.")
    @ApiResponse(responseCode = "200", description = "Invitations retrieved")
    @GetMapping
    public ResponseEntity<List<InvitationDetail>> getMyInvitations(@CurrentMember MemberDetails memberDetails) {
        List<InvitationDetail> response = invitationUseCase.getMyInvitations(memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }
}

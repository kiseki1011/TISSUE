package com.tissue.feature.workspace.web;

import com.tissue.feature.member.domain.exception.MemberErrorCode;
import com.tissue.feature.workspace.application.port.usecase.InvitationCommandUseCase;
import com.tissue.feature.workspace.domain.exception.WorkspaceErrorCode;
import com.tissue.global.openapi.MemberErrors;
import com.tissue.global.openapi.WorkspaceErrors;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Invitation")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/invitations")
public class InvitationCommandController {

    private final InvitationCommandUseCase invitationCommandUseCase;

    @Operation(
            operationId = "acceptInvitation",
            summary = "Accept invitation",
            description = "Accept a workspace invitation.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Invitation accepted"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @WorkspaceErrors({
        WorkspaceErrorCode.INVITATION_NOT_FOUND,
        WorkspaceErrorCode.WORKSPACE_ARCHIVED,
        WorkspaceErrorCode.WORKSPACE_MEMBER_LIMIT_EXCEEDED,
    })
    @MemberErrors({
        MemberErrorCode.MEMBER_NOT_FOUND,
        MemberErrorCode.MEMBER_DELETED,
        MemberErrorCode.WORKSPACE_JOIN_LIMIT_EXCEEDED,
    })
    @PostMapping("/{invitationId}:accept")
    public ResponseEntity<Void> acceptInvitation(
            @PathVariable Long invitationId, @CurrentMember MemberDetails memberDetails) {
        invitationCommandUseCase.accept(memberDetails.getMemberId(), invitationId);

        return ResponseEntity.noContent().build();
    }

    @Operation(
            operationId = "rejectInvitation",
            summary = "Reject invitation",
            description = "Reject a workspace invitation.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Invitation rejected"),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @WorkspaceErrors({
        WorkspaceErrorCode.INVITATION_NOT_FOUND,
    })
    @MemberErrors({
        MemberErrorCode.MEMBER_NOT_FOUND,
        MemberErrorCode.MEMBER_DELETED,
    })
    @PostMapping("/{invitationId}:reject")
    public ResponseEntity<Void> rejectInvitation(
            @PathVariable Long invitationId, @CurrentMember MemberDetails memberDetails) {
        invitationCommandUseCase.reject(memberDetails.getMemberId(), invitationId);

        return ResponseEntity.noContent().build();
    }
}

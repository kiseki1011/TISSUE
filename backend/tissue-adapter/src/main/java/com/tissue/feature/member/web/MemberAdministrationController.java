package com.tissue.feature.member.web;

import com.tissue.feature.member.application.port.usecase.MemberAdministrationUseCase;
import com.tissue.feature.member.domain.exception.MemberErrorCode;
import com.tissue.feature.member.web.request.AssignMemberTeamRequest;
import com.tissue.feature.organization.team.domain.exception.TeamErrorCode;
import com.tissue.global.openapi.MemberErrors;
import com.tissue.global.openapi.TeamErrors;
import com.tissue.shared.auth.CurrentMember;
import com.tissue.shared.auth.MemberDetails;
import com.tissue.shared.auth.RequireSystemAdmin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Member Administration")
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberAdministrationController {

    private final MemberAdministrationUseCase memberAdministrationUseCase;

    @Operation(operationId = "assignMemberTeam", summary = "Assign a member's team", description = """
                Assign a team to a member, or clear it by sending a `null` `teamId`.

                **Requirements:**
                - Requires system `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Team assigned"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @MemberErrors({
        MemberErrorCode.SYSTEM_ADMIN_REQUIRED,
        MemberErrorCode.MEMBER_NOT_FOUND,
        MemberErrorCode.MEMBER_DELETED,
    })
    @TeamErrors({TeamErrorCode.TEAM_NOT_FOUND})
    @RequireSystemAdmin
    @PatchMapping("/{memberId}/team")
    public ResponseEntity<Void> assignMemberTeam(
            @PathVariable Long memberId,
            @RequestBody @Valid AssignMemberTeamRequest request,
            @CurrentMember MemberDetails memberDetails) {
        memberAdministrationUseCase.assignTeam(memberId, request.teamId(), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }
}

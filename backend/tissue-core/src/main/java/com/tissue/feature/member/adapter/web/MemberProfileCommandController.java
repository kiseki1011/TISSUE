package com.tissue.feature.member.adapter.web;

import com.tissue.feature.member.adapter.web.request.UpdateMemberLanguageRequest;
import com.tissue.feature.member.adapter.web.request.UpdateMemberNameRequest;
import com.tissue.feature.member.adapter.web.request.UpdateMemberPositionRequest;
import com.tissue.feature.member.application.port.usecase.MemberProfileCommandUseCase;
import com.tissue.feature.member.domain.exception.MemberErrorCode;
import com.tissue.feature.organization.position.domain.exception.PositionErrorCode;
import com.tissue.global.openapi.MemberErrors;
import com.tissue.global.openapi.PositionErrors;
import com.tissue.shared.auth.CurrentMember;
import com.tissue.shared.auth.LocalAuthOnly;
import com.tissue.shared.auth.MemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Member Profile")
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberProfileCommandController {

    private final MemberProfileCommandUseCase memberProfileCommandUseCase;

    @Operation(
            operationId = "updateMemberName",
            summary = "Update name",
            description = "Change the current user's name. Unavailable in OIDC mode.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Name updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @MemberErrors({MemberErrorCode.MEMBER_NOT_FOUND, MemberErrorCode.MEMBER_DELETED})
    @LocalAuthOnly
    @PatchMapping("/name")
    public ResponseEntity<Void> updateMemberName(
            @RequestBody @Valid UpdateMemberNameRequest request, @CurrentMember MemberDetails memberDetails) {
        memberProfileCommandUseCase.updateName(request.newName(), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(
            operationId = "updateMemberLanguage",
            summary = "Update language",
            description = "Change the current user's preferred language.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Language updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @MemberErrors({MemberErrorCode.MEMBER_NOT_FOUND, MemberErrorCode.MEMBER_DELETED})
    @PatchMapping("/language")
    public ResponseEntity<Void> updateMemberLanguage(
            @RequestBody @Valid UpdateMemberLanguageRequest request, @CurrentMember MemberDetails memberDetails) {
        memberProfileCommandUseCase.updateLanguage(request.language(), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "updateMemberPosition", summary = "Set my position", description = """
                Set the current user's own position. Send a `null` `positionId` to clear it.""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Position updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @MemberErrors({MemberErrorCode.MEMBER_NOT_FOUND, MemberErrorCode.MEMBER_DELETED})
    @PositionErrors({PositionErrorCode.POSITION_NOT_FOUND})
    @PatchMapping("/position")
    public ResponseEntity<Void> updateMemberPosition(
            @RequestBody @Valid UpdateMemberPositionRequest request, @CurrentMember MemberDetails memberDetails) {
        memberProfileCommandUseCase.updatePosition(request.positionId(), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }
}

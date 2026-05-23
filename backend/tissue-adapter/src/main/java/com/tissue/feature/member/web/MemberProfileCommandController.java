package com.tissue.feature.member.web;

import com.tissue.feature.member.application.port.usecase.MemberProfileCommandUseCase;
import com.tissue.feature.member.domain.exception.MemberErrorCode;
import com.tissue.feature.member.web.request.UpdateMemberLanguageRequest;
import com.tissue.feature.member.web.request.UpdateMemberNameRequest;
import com.tissue.global.openapi.MemberErrors;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
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
            description = "Change the current user's name.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Name updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @MemberErrors({MemberErrorCode.MEMBER_NOT_FOUND, MemberErrorCode.MEMBER_DELETED})
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
}

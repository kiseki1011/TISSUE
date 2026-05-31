package com.tissue.feature.organization.position.web;

import com.tissue.feature.member.domain.exception.MemberErrorCode;
import com.tissue.feature.organization.position.application.dto.response.PositionResponse;
import com.tissue.feature.organization.position.application.port.usecase.PositionUseCase;
import com.tissue.feature.organization.position.domain.exception.PositionErrorCode;
import com.tissue.feature.organization.position.web.request.CreatePositionRequest;
import com.tissue.feature.organization.position.web.request.UpdatePositionRequest;
import com.tissue.global.openapi.MemberErrors;
import com.tissue.global.openapi.PositionErrors;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Position")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PositionController {

    private final PositionUseCase positionUseCase;

    @Operation(operationId = "createPosition", summary = "Create position", description = """
                Create a new global position (ex: "Backend Engineer").

                **Requirements:**
                - Requires system `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Position created"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @MemberErrors({MemberErrorCode.SYSTEM_ADMIN_REQUIRED})
    @PositionErrors({PositionErrorCode.DUPLICATE_POSITION_NAME})
    @RequireSystemAdmin
    @PostMapping("/positions")
    public ResponseEntity<PositionResponse> createPosition(
            @RequestBody @Valid CreatePositionRequest req, @CurrentMember MemberDetails memberDetails) {
        PositionResponse response = positionUseCase.create(req.toCommand(), memberDetails.getMemberId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(operationId = "updatePosition", summary = "Update position", description = """
                Update a position's name, description, or color. Only provided fields are updated.

                **Requirements:**
                - Requires system `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Position updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @MemberErrors({MemberErrorCode.SYSTEM_ADMIN_REQUIRED})
    @PositionErrors({
        PositionErrorCode.POSITION_NOT_FOUND,
        PositionErrorCode.DUPLICATE_POSITION_NAME,
    })
    @RequireSystemAdmin
    @PatchMapping("/positions/{positionId}")
    public ResponseEntity<Void> updatePosition(
            @PathVariable Long positionId,
            @RequestBody @Valid UpdatePositionRequest request,
            @CurrentMember MemberDetails memberDetails) {
        positionUseCase.update(positionId, request.toCommand(), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "deletePosition", summary = "Delete position", description = """
                Permanently delete a global position. Any member currently assigned to it is unassigned.

                **Requirements:**
                - Requires system `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Position deleted"),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @MemberErrors({MemberErrorCode.SYSTEM_ADMIN_REQUIRED})
    @PositionErrors({PositionErrorCode.POSITION_NOT_FOUND})
    @RequireSystemAdmin
    @DeleteMapping("/positions/{positionId}")
    public ResponseEntity<Void> deletePosition(
            @PathVariable Long positionId, @CurrentMember MemberDetails memberDetails) {
        positionUseCase.delete(positionId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }
}

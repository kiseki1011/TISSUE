package com.tissue.feature.organization.position.web;

import com.tissue.feature.organization.position.application.dto.response.PositionCreateResponse;
import com.tissue.feature.organization.position.application.dto.response.PositionDetail;
import com.tissue.feature.organization.position.application.dto.response.PositionDetailList;
import com.tissue.feature.organization.position.application.port.usecase.PositionUseCase;
import com.tissue.feature.organization.position.domain.exception.PositionErrorCode;
import com.tissue.feature.organization.position.web.request.CreatePositionRequest;
import com.tissue.feature.organization.position.web.request.UpdatePositionRequest;
import com.tissue.feature.workspace.domain.exception.WorkspaceErrorCode;
import com.tissue.global.openapi.PositionErrors;
import com.tissue.global.openapi.WorkspaceErrors;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Position")
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/positions")
@RequiredArgsConstructor
public class PositionController {

    private final PositionUseCase positionUseCase;

    @Operation(operationId = "createPosition", summary = "Create position", description = """
                Create a new position within a workspace.

                **Requirements:**
                - Requires workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Position created"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @WorkspaceErrors({
        WorkspaceErrorCode.WORKSPACE_MEMBER_NOT_FOUND,
        WorkspaceErrorCode.WORKSPACE_NOT_FOUND,
        WorkspaceErrorCode.WORKSPACE_ARCHIVED,
        WorkspaceErrorCode.INSUFFICIENT_WORKSPACE_ROLE,
    })
    @PositionErrors({PositionErrorCode.DUPLICATE_POSITION_NAME})
    @PostMapping
    public ResponseEntity<PositionCreateResponse> createPosition(
            @PathVariable String workspaceKey,
            @Valid @RequestBody CreatePositionRequest request,
            @CurrentMember MemberDetails memberDetails) {
        var command = request.toCommand();
        PositionCreateResponse response = positionUseCase.create(workspaceKey, command, memberDetails.getMemberId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(operationId = "updatePosition", summary = "Update position", description = """
                Update a position's name or description. Only provided fields are updated.

                **Requirements:**
                - Requires workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Position updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @WorkspaceErrors({
        WorkspaceErrorCode.WORKSPACE_MEMBER_NOT_FOUND,
        WorkspaceErrorCode.INSUFFICIENT_WORKSPACE_ROLE,
        WorkspaceErrorCode.WORKSPACE_ARCHIVED,
    })
    @PositionErrors({
        PositionErrorCode.POSITION_NOT_FOUND,
        PositionErrorCode.DUPLICATE_POSITION_NAME,
    })
    @PatchMapping("/{positionId}")
    public ResponseEntity<Void> updatePosition(
            @PathVariable String workspaceKey,
            @PathVariable Long positionId,
            @Valid @RequestBody UpdatePositionRequest request,
            @CurrentMember MemberDetails memberDetails) {
        var command = request.toCommand();
        positionUseCase.update(workspaceKey, positionId, command, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "deletePosition", summary = "Delete position", description = """
                Permanently delete a position from the workspace.

                **Requirements:**
                - Requires workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Position deleted"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @WorkspaceErrors({
        WorkspaceErrorCode.WORKSPACE_MEMBER_NOT_FOUND,
        WorkspaceErrorCode.INSUFFICIENT_WORKSPACE_ROLE,
        WorkspaceErrorCode.WORKSPACE_ARCHIVED,
    })
    @PositionErrors({
        PositionErrorCode.POSITION_NOT_FOUND,
        PositionErrorCode.POSITION_IN_USE,
    })
    @DeleteMapping("/{positionId}")
    public ResponseEntity<Void> deletePosition(
            @PathVariable String workspaceKey,
            @PathVariable Long positionId,
            @CurrentMember MemberDetails memberDetails) {
        positionUseCase.delete(workspaceKey, positionId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(
            operationId = "getPosition",
            summary = "Get position detail",
            description = "Retrieve the detail of a position.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Position detail retrieved"),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @WorkspaceErrors({WorkspaceErrorCode.WORKSPACE_MEMBER_NOT_FOUND})
    @PositionErrors({PositionErrorCode.POSITION_NOT_FOUND})
    @GetMapping("/{positionId}")
    public ResponseEntity<PositionDetail> getPosition(
            @PathVariable String workspaceKey,
            @PathVariable Long positionId,
            @CurrentMember MemberDetails memberDetails) {
        PositionDetail response = positionUseCase.getPosition(workspaceKey, positionId, memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @Operation(
            operationId = "listPositions",
            summary = "List positions",
            description = "Retrieve all positions in the workspace.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Positions retrieved"),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @WorkspaceErrors({WorkspaceErrorCode.WORKSPACE_MEMBER_NOT_FOUND})
    @GetMapping
    public ResponseEntity<PositionDetailList> listPositions(
            @PathVariable String workspaceKey, @CurrentMember MemberDetails memberDetails) {
        PositionDetailList response = positionUseCase.getWorkspacePositions(workspaceKey, memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }
}

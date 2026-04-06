package com.tissue.feature.organization.position.web;

import com.tissue.feature.organization.position.application.dto.response.PositionCreateResponse;
import com.tissue.feature.organization.position.application.dto.response.PositionDetail;
import com.tissue.feature.organization.position.application.dto.response.PositionDetailList;
import com.tissue.feature.organization.position.application.port.usecase.PositionUseCase;
import com.tissue.feature.organization.position.web.request.CreatePositionRequest;
import com.tissue.feature.organization.position.web.request.UpdatePositionRequest;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Tag(name = "Position")
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/positions")
@RequiredArgsConstructor
public class PositionController {

    private final PositionUseCase positionUseCase;

    @Operation(summary = "Create position", description = """
                Create a new position within a workspace.

                **Requirements:**
                - Requires workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Position created"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "409", description = "Position name already exists", content = @Content)
    })
    @PostMapping
    public ResponseEntity<PositionCreateResponse> createPosition(
            @PathVariable String workspaceKey,
            @Valid @RequestBody CreatePositionRequest request,
            @CurrentMember MemberDetails memberDetails) {
        var command = request.toCommand();
        PositionCreateResponse response = positionUseCase.create(workspaceKey, command, memberDetails.getMemberId());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{positionId}")
                .buildAndExpand(response.positionId())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Update position", description = """
                Update a position's name or description. Only provided fields are updated.

                **Requirements:**
                - Requires workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Position updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Position not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Position name already exists", content = @Content)
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

    @Operation(summary = "Delete position", description = """
                Delete a position from the workspace.

                **Requirements:**
                - Requires workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Position deleted"),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Position not found", content = @Content)
    })
    @DeleteMapping("/{positionId}")
    public ResponseEntity<Void> deletePosition(
            @PathVariable String workspaceKey,
            @PathVariable Long positionId,
            @CurrentMember MemberDetails memberDetails) {
        positionUseCase.delete(workspaceKey, positionId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get position detail", description = "Retrieve the detail of a position.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Position detail retrieved"),
        @ApiResponse(responseCode = "404", description = "Position not found", content = @Content)
    })
    @GetMapping("/{positionId}")
    public ResponseEntity<PositionDetail> getPositionDetail(
            @PathVariable String workspaceKey,
            @PathVariable Long positionId,
            @CurrentMember MemberDetails memberDetails) {
        PositionDetail response = positionUseCase.getPosition(workspaceKey, positionId, memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List positions", description = "Retrieve all positions in the workspace.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Positions retrieved"),
        @ApiResponse(responseCode = "404", description = "Workspace not found", content = @Content)
    })
    @GetMapping
    public ResponseEntity<PositionDetailList> getPositions(
            @PathVariable String workspaceKey, @CurrentMember MemberDetails memberDetails) {
        PositionDetailList response = positionUseCase.getWorkspacePositions(workspaceKey, memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }
}

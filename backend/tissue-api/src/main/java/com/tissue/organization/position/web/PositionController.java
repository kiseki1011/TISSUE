package com.tissue.organization.position.web;

import com.tissue.feature.organization.position.application.dto.response.PositionCreateResponse;
import com.tissue.feature.organization.position.application.dto.response.PositionDetail;
import com.tissue.feature.organization.position.application.dto.response.PositionDetailList;
import com.tissue.feature.organization.position.application.port.usecase.PositionUseCase;
import com.tissue.feature.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.organization.position.web.request.CreatePositionRequest;
import com.tissue.organization.position.web.request.UpdatePositionRequest;
import com.tissue.workspace.web.resolver.CurrentWorkspaceMember;
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

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/positions")
@RequiredArgsConstructor
public class PositionController {

    private final PositionUseCase positionUseCase;

    @PostMapping
    public ResponseEntity<PositionCreateResponse> createPosition(
            @Valid @RequestBody CreatePositionRequest request,
            @CurrentWorkspaceMember WorkspaceMemberContext actorContext) {
        var command = request.toCommand();
        PositionCreateResponse response = positionUseCase.create(command, actorContext);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{positionId}")
                .buildAndExpand(response.positionId())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @PatchMapping("/{positionId}")
    public ResponseEntity<Void> updatePosition(
            @PathVariable Long positionId,
            @Valid @RequestBody UpdatePositionRequest request,
            @CurrentWorkspaceMember WorkspaceMemberContext actorContext) {
        var command = request.toCommand();
        positionUseCase.update(positionId, command, actorContext);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{positionId}")
    public ResponseEntity<Void> deletePosition(
            @PathVariable Long positionId, @CurrentWorkspaceMember WorkspaceMemberContext actorContext) {
        positionUseCase.delete(positionId, actorContext);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{positionId}")
    public ResponseEntity<PositionDetail> getPositionDetail(
            @PathVariable Long positionId, @CurrentWorkspaceMember WorkspaceMemberContext actorContext) {
        PositionDetail response = positionUseCase.getPosition(positionId, actorContext);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PositionDetailList> getPositions(
            @CurrentWorkspaceMember WorkspaceMemberContext actorContext) {
        PositionDetailList response = positionUseCase.getWorkspacePositions(actorContext);
        return ResponseEntity.ok(response);
    }
}

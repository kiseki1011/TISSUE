package com.tissue.position.adapter.in.web;

import com.tissue.position.adapter.in.web.request.CreatePositionRequest;
import com.tissue.position.adapter.in.web.request.UpdatePositionRequest;
import com.tissue.position.application.dto.response.GetPositions;
import com.tissue.position.application.dto.response.PositionCreateResponse;
import com.tissue.position.application.dto.response.PositionDetail;
import com.tissue.position.application.port.in.PositionUseCase;
import com.tissue.workspace.adapter.in.web.resolver.CurrentWorkspaceMember;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;
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

        var command = request.toCommand(actorContext);
        PositionCreateResponse response = positionUseCase.create(command);

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

        var command = request.toCommand(positionId, actorContext);
        positionUseCase.update(command);

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

        PositionDetail response = positionUseCase.getPositionDetail(positionId, actorContext);
        return ResponseEntity.ok(response);
    }

    // TODO: should i change this into a pagination api?
    //  i think the number of positions in a single workspace going over 100 will be a rare case,
    //  but who knows?
    @GetMapping
    public ResponseEntity<GetPositions> getPositions(@CurrentWorkspaceMember WorkspaceMemberContext actorContext) {

        GetPositions response = positionUseCase.getPositions(actorContext);
        return ResponseEntity.ok(response);
    }
}

package com.tissue.position.adapter.in.web;

import com.tissue.position.adapter.in.web.request.CreatePositionRequest;
import com.tissue.position.adapter.in.web.request.UpdatePositionRequest;
import com.tissue.position.application.dto.response.GetPositions;
import com.tissue.position.application.dto.response.PositionCreateResponse;
import com.tissue.position.application.dto.response.PositionDetail;
import com.tissue.position.application.port.in.PositionUseCase;
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
            @PathVariable String workspaceKey, @Valid @RequestBody CreatePositionRequest request) {
        var command = request.toCommand(workspaceKey);
        PositionCreateResponse response = positionUseCase.create(command);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{positionId}")
                .buildAndExpand(response.positionId())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @PatchMapping("/{positionId}")
    public ResponseEntity<Void> updatePosition(
            @PathVariable String workspaceKey,
            @PathVariable Long positionId,
            @Valid @RequestBody UpdatePositionRequest request) {
        var command = request.toCommand(workspaceKey, positionId);
        positionUseCase.update(command);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{positionId}")
    public ResponseEntity<Void> deletePosition(@PathVariable String workspaceKey, @PathVariable Long positionId) {
        positionUseCase.delete(workspaceKey, positionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{positionId}")
    public ResponseEntity<PositionDetail> getPositionDetail(
            @PathVariable String workspaceKey, @PathVariable Long positionId) {
        PositionDetail response = positionUseCase.getPositionDetail(workspaceKey, positionId);
        return ResponseEntity.ok(response);
    }

    // TODO: should i change this into a pagination api?
    //  i think the number of positions in a single workspace going over 100 will be a rare case,
    // but
    // who knows?
    @GetMapping
    public ResponseEntity<GetPositions> getPositions(@PathVariable String workspaceKey) {
        GetPositions response = positionUseCase.getPositions(workspaceKey);
        return ResponseEntity.ok(response);
    }
}

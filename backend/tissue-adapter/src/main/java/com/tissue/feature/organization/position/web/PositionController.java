package com.tissue.feature.organization.position.web;

import com.tissue.feature.organization.position.application.dto.response.PositionCreateResponse;
import com.tissue.feature.organization.position.application.dto.response.PositionDetail;
import com.tissue.feature.organization.position.application.dto.response.PositionDetailList;
import com.tissue.feature.organization.position.application.port.usecase.PositionUseCase;
import com.tissue.feature.organization.position.web.request.CreatePositionRequest;
import com.tissue.feature.organization.position.web.request.UpdatePositionRequest;
import com.tissue.principal.CurrentMember;
import com.tissue.principal.MemberDetails;
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

    @DeleteMapping("/{positionId}")
    public ResponseEntity<Void> deletePosition(
            @PathVariable String workspaceKey,
            @PathVariable Long positionId,
            @CurrentMember MemberDetails memberDetails) {

        positionUseCase.delete(workspaceKey, positionId, memberDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{positionId}")
    public ResponseEntity<PositionDetail> getPositionDetail(
            @PathVariable String workspaceKey,
            @PathVariable Long positionId,
            @CurrentMember MemberDetails memberDetails) {

        PositionDetail response = positionUseCase.getPosition(workspaceKey, positionId, memberDetails.getMemberId());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PositionDetailList> getPositions(
            @PathVariable String workspaceKey, @CurrentMember MemberDetails memberDetails) {

        PositionDetailList response = positionUseCase.getWorkspacePositions(workspaceKey, memberDetails.getMemberId());
        return ResponseEntity.ok(response);
    }
}

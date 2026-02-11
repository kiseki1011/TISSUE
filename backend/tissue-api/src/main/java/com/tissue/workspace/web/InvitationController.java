package com.tissue.workspace.web;

import com.tissue.feature.workspace.application.dto.response.query.InvitationDetail;
import com.tissue.feature.workspace.application.port.usecase.InvitationUseCase;
import com.tissue.principal.MemberDetails;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/invitations")
public class InvitationController {

    private final InvitationUseCase invitationUseCase;

    @PostMapping("/{invitationId}/accept")
    public ResponseEntity<Void> accept(
            @PathVariable Long invitationId, @AuthenticationPrincipal MemberDetails userDetails) {

        invitationUseCase.accept(userDetails.getMemberId(), invitationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{invitationId}/reject")
    public ResponseEntity<Void> reject(
            @PathVariable Long invitationId, @AuthenticationPrincipal MemberDetails userDetails) {

        invitationUseCase.reject(userDetails.getMemberId(), invitationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<InvitationDetail>> getMyInvitations(@AuthenticationPrincipal MemberDetails userDetails) {

        List<InvitationDetail> response = invitationUseCase.getMyInvitations(userDetails.getMemberId());
        return ResponseEntity.ok(response);
    }
}

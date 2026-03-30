package com.tissue.feature.workspace.web;

import com.tissue.feature.workspace.application.dto.response.query.InvitationDetail;
import com.tissue.feature.workspace.application.port.usecase.InvitationUseCase;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<Void> accept(@PathVariable Long invitationId, @CurrentMember MemberDetails memberDetails) {
        invitationUseCase.accept(memberDetails.getMemberId(), invitationId);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{invitationId}/reject")
    public ResponseEntity<Void> reject(@PathVariable Long invitationId, @CurrentMember MemberDetails memberDetails) {
        invitationUseCase.reject(memberDetails.getMemberId(), invitationId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<InvitationDetail>> getMyInvitations(@CurrentMember MemberDetails memberDetails) {
        List<InvitationDetail> response = invitationUseCase.getMyInvitations(memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }
}

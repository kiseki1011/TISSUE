package com.tissue.workspace.adapter.in.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.security.authentication.MemberUserDetails;
import com.tissue.security.authentication.resolver.CurrentMember;
import com.tissue.workspace.application.dto.response.query.InvitationDetail;
import com.tissue.workspace.application.port.in.InvitationUseCase;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/invitations")
public class InvitationController {

	private final InvitationUseCase invitationUseCase;

	@PostMapping("/{invitationId}/accept")
	public ResponseEntity<Void> accept(
		@PathVariable Long invitationId,
		@CurrentMember MemberUserDetails userDetails
	) {
		invitationUseCase.accept(userDetails.getMemberId(), invitationId);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{invitationId}/reject")
	public ResponseEntity<Void> reject(
		@PathVariable Long invitationId,
		@CurrentMember MemberUserDetails userDetails
	) {
		invitationUseCase.reject(userDetails.getMemberId(), invitationId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping
	public ResponseEntity<List<InvitationDetail>> getMyInvitations(
		@CurrentMember MemberUserDetails userDetails
	) {
		List<InvitationDetail> response = invitationUseCase.getMyInvitations(userDetails.getMemberId());
		return ResponseEntity.ok(response);
	}
}

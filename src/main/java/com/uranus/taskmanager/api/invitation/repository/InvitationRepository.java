package com.uranus.taskmanager.api.invitation.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.uranus.taskmanager.api.invitation.domain.Invitation;
import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.workspace.domain.Workspace;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {
	Optional<Invitation> findByWorkspaceAndMember(Workspace workspace, Member member);

	Optional<Invitation> findByWorkspaceCodeAndMemberLoginId(String workspaceCode, String memberLoginId);

	Optional<Invitation> findByWorkspaceCodeAndMemberId(String workspaceCode, Long memberId);
}

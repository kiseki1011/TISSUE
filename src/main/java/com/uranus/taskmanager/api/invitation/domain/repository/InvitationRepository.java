package com.uranus.taskmanager.api.invitation.domain.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.uranus.taskmanager.api.invitation.InvitationStatus;
import com.uranus.taskmanager.api.invitation.domain.Invitation;
import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.workspace.domain.Workspace;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {
	Optional<Invitation> findByWorkspaceAndMember(Workspace workspace, Member member);

	Optional<Invitation> findByWorkspaceCodeAndMemberId(String workspaceCode, Long memberId);

	@Query("SELECT i FROM Invitation i "
		+ "WHERE i.status = :status "
		+ "AND i.workspace.code = :workspaceCode "
		+ "AND i.member.id = :memberId")
	Optional<Invitation> findByStatusAndWorkspaceCodeAndMemberId(
		@Param("status") InvitationStatus status,
		@Param("workspaceCode") String workspaceCode,
		@Param("memberId") Long memberId
	);
}

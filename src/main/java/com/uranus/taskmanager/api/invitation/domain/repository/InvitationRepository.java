package com.uranus.taskmanager.api.invitation.domain.repository;

import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.uranus.taskmanager.api.invitation.domain.Invitation;
import com.uranus.taskmanager.api.invitation.domain.InvitationStatus;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

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

	@Query("SELECT DISTINCT m.id FROM WorkspaceMember wm JOIN wm.member m "
		+ "WHERE wm.workspace.id = :workspaceId "
		+ "UNION "
		+ "SELECT DISTINCT m.id FROM Invitation i JOIN i.member m "
		+ "WHERE i.workspace.id = :workspaceId AND i.status = 'PENDING'")
	Set<Long> findExistingMemberIds(@Param("workspaceId") Long workspaceId);
}

package com.tissue.api.invitation.infrastructure.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tissue.api.invitation.domain.model.Invitation;
import com.tissue.api.invitation.domain.model.enums.InvitationStatus;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

	Optional<Invitation> findByWorkspaceCodeAndMemberId(
		String workspaceCode,
		Long memberId
	);

	Optional<Invitation> findByIdAndStatus(
		Long id,
		InvitationStatus status
	);

	List<Invitation> findAllByMemberId(Long id);

	@Query("SELECT DISTINCT m.id FROM WorkspaceMember wm JOIN wm.member m "
		+ "WHERE wm.workspace.code = :workspaceCode "
		+ "UNION "
		+ "SELECT DISTINCT m.id FROM Invitation i JOIN i.member m "
		+ "WHERE i.workspace.code = :workspaceCode AND i.status = 'PENDING'")
	Set<Long> findExistingMemberIds(
		@Param("workspaceCode") String workspaceCode
	);

	@Modifying
	@Query("DELETE FROM Invitation i "
		+ "WHERE i.member.id = :memberId "
		+ "AND i.status IN :statuses")
	void deleteAllByMemberIdAndStatusIn(
		@Param("memberId") Long memberId,
		@Param("statuses") List<InvitationStatus> statuses
	);
}

package com.tissue.api.invitation.infrastructure.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tissue.api.invitation.domain.Invitation;
import com.tissue.api.invitation.domain.enums.InvitationStatus;

public interface InvitationQueryRepository extends JpaRepository<Invitation, Long> {

	@Query("SELECT i FROM Invitation i "
		+ "WHERE i.member.id = :memberId "
		+ "AND i.status IN :statuses")
	Page<Invitation> findAllByMemberIdAndStatusIn(
		@Param("memberId") Long memberId,
		@Param("statuses") List<InvitationStatus> statuses,
		Pageable pageable
	);
}

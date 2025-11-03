package com.tissue.api.workspacemember.infrastructure.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.tissue.api.workspacemember.domain.model.WorkspaceMember;

public interface WorkspaceMemberQueryRepository extends Repository<WorkspaceMember, Long> {

	Optional<WorkspaceMember> findByMember_IdAndWorkspace_Key(
		Long memberId,
		String workspaceKey
	);

	@Query("""
		    SELECT wm
		    FROM WorkspaceMember wm
		    JOIN FETCH wm.member m
		    JOIN FETCH wm.workspace w
		    WHERE m.id = :memberId
		      AND w.key = :workspaceKey
		      AND wm.archived = false
		""")
	Optional<WorkspaceMember> find(
		Long memberId,
		String workspaceKey
	);

	@Query("""
		    SELECT wm
		    FROM WorkspaceMember wm
		    JOIN FETCH wm.member m
		    JOIN FETCH wm.workspace w
		    WHERE m.id = :memberId
		      AND w.key = :workspaceKey
		""")
	Optional<WorkspaceMember> findIncludingArchived(
		@Param("memberId") Long memberId,
		@Param("workspaceKey") String workspaceKey
	);
}

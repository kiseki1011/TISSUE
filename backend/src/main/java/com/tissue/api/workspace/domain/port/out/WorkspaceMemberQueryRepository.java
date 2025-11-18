package com.tissue.api.workspace.domain.port.out;

import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.tissue.api.member.domain.model.Member;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.domain.WorkspaceMember;

public interface WorkspaceMemberQueryRepository extends Repository<WorkspaceMember, Long> {

	Optional<WorkspaceMember> findByMember_IdAndWorkspace_Key(
		Long memberId,
		String workspaceKey
	);

	Optional<WorkspaceMember> findByMember_IdAndWorkspace(
		Long memberId,
		Workspace workspace
	);

	Optional<WorkspaceMember> findByMemberAndWorkspace(
		Member member,
		Workspace workspace
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
	Optional<WorkspaceMember> findByMemberIdAndWorkspaceKey(
		Long memberId,
		String workspaceKey
	);

	// TODO: 메서드명 괜찮나?
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

package com.tissue.api.workspace.domain.port.out;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.tissue.api.member.domain.model.Member;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.domain.WorkspaceMember;

public interface WorkspaceMemberCommandRepository extends Repository<WorkspaceMember, Long> {

	Optional<WorkspaceMember> findByMember_IdAndWorkspaceKey(Long memberId, String workspaceKey);

	Optional<WorkspaceMember> findByMemberAndWorkspace(Member member, Workspace workspace);

	List<WorkspaceMember> findAllByWorkspace_Key(String workspaceKey);

	@Query("SELECT wm FROM WorkspaceMember wm WHERE wm.workspace.key = :workspaceKey AND wm.role IN ('ADMIN', 'OWNER')")
	Set<WorkspaceMember> findAdminsByWorkspace_Key(@Param("workspaceKey") String workspaceKey);
}

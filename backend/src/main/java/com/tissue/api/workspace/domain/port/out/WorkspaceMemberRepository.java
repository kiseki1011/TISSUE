package com.tissue.api.workspace.domain.port.out;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tissue.api.member.domain.model.Member;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.domain.WorkspaceMember;
import com.tissue.api.workspace.domain.enums.WorkspaceRole;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {

	Page<WorkspaceMember> findByMember_Id(Long memberId, Pageable pageable);

	Optional<WorkspaceMember> findByMember_IdAndWorkspace_Key(Long memberId, String workspaceKey);

	Optional<WorkspaceMember> findByMemberAndWorkspace(Member member, Workspace workspace);

	boolean existsByMemberAndRole(Member member, WorkspaceRole role);

	boolean existsByMember_IdAndWorkspace_Key(Long memberId, String workspaceKey);

	List<WorkspaceMember> findAllByWorkspace_Key(String workspaceKey);

	@Query("SELECT wm FROM WorkspaceMember wm WHERE wm.workspace.key = :workspaceKey AND wm.role IN ('ADMIN', 'OWNER')")
	Set<WorkspaceMember> findAdminsByWorkspace_Key(@Param("workspaceKey") String workspaceKey);
}

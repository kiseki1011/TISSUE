package com.tissue.api.workspace.application.port.out;

import java.util.List;

import org.springframework.data.repository.Repository;

import com.tissue.api.workspace.domain.WorkspaceMember;

public interface WorkspaceMemberCommandRepository extends Repository<WorkspaceMember, Long> {

	WorkspaceMember save(WorkspaceMember workspaceMember);

	List<WorkspaceMember> saveAll(Iterable<WorkspaceMember> workspaceMembers);
}

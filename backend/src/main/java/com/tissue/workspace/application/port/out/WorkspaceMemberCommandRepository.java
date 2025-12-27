package com.tissue.workspace.application.port.out;

import com.tissue.workspace.domain.WorkspaceMember;
import java.util.List;
import org.springframework.data.repository.Repository;

public interface WorkspaceMemberCommandRepository extends Repository<WorkspaceMember, Long> {

    WorkspaceMember save(WorkspaceMember workspaceMember);

    List<WorkspaceMember> saveAll(Iterable<WorkspaceMember> workspaceMembers);
}

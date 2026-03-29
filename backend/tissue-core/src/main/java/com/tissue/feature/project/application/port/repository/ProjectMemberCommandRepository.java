package com.tissue.feature.project.application.port.repository;

import com.tissue.feature.project.domain.ProjectMember;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface ProjectMemberCommandRepository extends Repository<ProjectMember, Long> {

    ProjectMember save(ProjectMember projectMember);

    List<ProjectMember> saveAll(Iterable<ProjectMember> projectMembers);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
           UPDATE ProjectMember pm
           SET pm.softDeleted = true, pm.softDeletedAt = CURRENT_TIMESTAMP,
               pm.archived = true, pm.archivedAt = CURRENT_TIMESTAMP
           WHERE pm.workspaceKey = :workspaceKey
             AND pm.memberId = :memberId
       """)
    void softDeleteAllByWorkspaceKeyAndMemberId(
            @Param("workspaceKey") String workspaceKey, @Param("memberId") Long memberId);
}

package com.tissue.feature.project.application.port.repository;

import com.tissue.feature.project.domain.ProjectMember;
import java.util.List;
import org.springframework.data.repository.Repository;

public interface ProjectMemberCommandRepository extends Repository<ProjectMember, Long> {

    ProjectMember save(ProjectMember projectMember);

    List<ProjectMember> saveAll(Iterable<ProjectMember> projectMembers);
}

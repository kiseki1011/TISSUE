package com.tissue.project.application.port.out;

import com.tissue.project.domain.ProjectMember;
import java.util.List;
import org.springframework.data.repository.Repository;

public interface ProjectMemberCommandRepository extends Repository<ProjectMember, Long> {

    ProjectMember save(ProjectMember projectMember);

    List<ProjectMember> saveAll(Iterable<ProjectMember> projectMembers);
}

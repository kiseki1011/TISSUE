package com.tissue.project.application.port.out;

import java.util.List;

import org.springframework.data.repository.Repository;

import com.tissue.project.domain.ProjectMember;

public interface ProjectMemberCommandRepository extends Repository<ProjectMember, Long> {

	ProjectMember save(ProjectMember projectMember);

	List<ProjectMember> saveAll(Iterable<ProjectMember> projectMembers);
}

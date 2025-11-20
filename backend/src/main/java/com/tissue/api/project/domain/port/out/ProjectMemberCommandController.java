package com.tissue.api.project.domain.port.out;

import java.util.List;

import org.springframework.data.repository.Repository;

import com.tissue.api.project.domain.ProjectMember;

public interface ProjectMemberCommandController extends Repository<ProjectMember, Long> {

	List<ProjectMember> saveAll(Iterable<ProjectMember> projectMembers);
}

package com.tissue.api.project.application.service.finder;

import java.util.Collection;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.tissue.api.project.domain.Project;
import com.tissue.api.project.domain.ProjectMember;
import com.tissue.api.project.domain.exception.ProjectMemberNotFoundException;
import com.tissue.api.project.domain.port.out.ProjectMemberQueryRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProjectMemberFinder {

	private final ProjectMemberQueryRepository queryRepository;

	// public ProjectMember findBy(
	// 	Project project,
	// 	Long memberId
	// ) {
	// 	return queryRepository.findByProjectAndMemberId(project, memberId)
	// 		.orElseThrow(
	// 			() -> new ProjectMemberNotFoundException(project.getWorkspaceKey(), project.getKey(), memberId)
	// 		);
	// }

	public ProjectMember findBy(
		Project project,
		Long memberId
	) {
		return queryRepository.findAnyByProjectIdAndMemberId(project.getId(), memberId)
			.orElseThrow(
				() -> new ProjectMemberNotFoundException(project.getWorkspaceKey(), project.getKey(), memberId)
			);
	}

	public Set<Long> findExistingMemberIdsBy(Project project, Collection<Long> memberIds) {
		return queryRepository.findMemberIdsByProjectAndMemberIds(project, memberIds);
	}
}

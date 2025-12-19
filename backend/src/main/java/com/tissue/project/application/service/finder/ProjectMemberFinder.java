package com.tissue.project.application.service.finder;

import java.util.Collection;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.tissue.project.application.port.out.ProjectMemberQueryRepository;
import com.tissue.project.domain.Project;
import com.tissue.project.domain.ProjectMember;
import com.tissue.project.domain.exception.ProjectMemberNotFoundException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProjectMemberFinder {

	private final ProjectMemberQueryRepository queryRepository;

	public boolean existsBy(Project project, Long memberId) {
		return queryRepository.existsByProjectAndMemberId(project, memberId);
	}

	// TODO: WorkspaceMember를 같이 가져오기(JOIN FETCH)
	public ProjectMember findBy(Project project, Long memberId) {
		return queryRepository.findAnyByProjectIdAndMemberId(project.getId(), memberId)
			.orElseThrow(
				() -> new ProjectMemberNotFoundException(project.getWorkspaceKey(), project.getKey(), memberId)
			);
	}

	public Set<Long> findExistingMemberIdsBy(Project project, Collection<Long> memberIds) {
		return queryRepository.findMemberIdsByProjectAndMemberIds(project, memberIds);
	}
}

package com.tissue.project.application.service.validator;

import org.springframework.stereotype.Component;

import com.tissue.project.domain.Project;
import com.tissue.project.domain.exception.ProjectMemberAlreadyExistsException;
import com.tissue.project.application.port.out.ProjectMemberQueryRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProjectMemberValidator {

	private final ProjectMemberQueryRepository projectMemberRepository;

	public void ensureNotAlreadyJoined(Project project, Long memberId) {
		if (projectMemberRepository.existsByProjectAndMemberId(project, memberId)) {
			throw new ProjectMemberAlreadyExistsException(project.getWorkspaceKey(), project.getKey(), memberId);
		}
	}
}

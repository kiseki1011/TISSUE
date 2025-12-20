package com.tissue.project.application.service.validator;

import org.springframework.stereotype.Component;

import com.tissue.project.application.port.out.ProjectMemberQueryRepository;
import com.tissue.project.domain.Project;
import com.tissue.project.domain.exception.ProjectExceptions;

import lombok.RequiredArgsConstructor;

// TODO: should i just integrate this into ProjectValidator?
@Component
@RequiredArgsConstructor
public class ProjectMemberValidator {

	private final ProjectMemberQueryRepository projectMemberRepository;

	public void ensureNotAlreadyJoined(Project project, Long memberId) {
		if (projectMemberRepository.existsByProjectAndMemberId(project, memberId)) {
			throw ProjectExceptions.memberAlreadyExists(project, memberId);
		}
	}
}

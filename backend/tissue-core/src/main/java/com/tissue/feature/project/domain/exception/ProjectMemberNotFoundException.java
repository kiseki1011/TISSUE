package com.tissue.feature.project.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.MEMBER_ID;
import static com.tissue.shared.exception.ErrorContextKeys.PROJECT_KEY;

import com.tissue.shared.exception.base.ResourceNotFoundException;

public class ProjectMemberNotFoundException extends ResourceNotFoundException {

    public ProjectMemberNotFoundException(String projectKey, Long memberId) {
        super(ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND);
        addContext(PROJECT_KEY, projectKey);
        addContext(MEMBER_ID, memberId);
    }
}

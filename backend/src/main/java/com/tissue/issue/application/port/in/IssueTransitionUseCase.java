package com.tissue.issue.application.port.in;

import static com.tissue.issue.application.service.authorization.IssueAuthExpressions.*;

import com.tissue.issue.application.dto.request.PerformTransitionCommand;
import org.springframework.security.access.prepost.PreAuthorize;

public interface IssueTransitionUseCase {

    @PreAuthorize(REQUIRES_ISSUE_EDIT_PERMISSION)
    void performTransition(PerformTransitionCommand cmd);
}

package com.tissue.issue.application.port.in;

import com.tissue.issue.application.dto.request.PerformSystemTransitionCommand;
import com.tissue.issue.application.dto.request.PerformTransitionCommand;

public interface IssueTransitionUseCase {

    void performTransition(PerformTransitionCommand cmd);

    void performTransitionBySystem(PerformSystemTransitionCommand cmd);
}

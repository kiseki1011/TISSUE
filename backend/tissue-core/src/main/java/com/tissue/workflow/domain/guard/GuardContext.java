package com.tissue.workflow.domain.guard;

import com.tissue.issue.domain.Issue;
import com.tissue.workflow.domain.WorkflowTransition;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

@Getter
@Builder
public class GuardContext {
    private final Issue issue;
    private final WorkflowTransition transition;
    private final @Nullable Long actorMemberId;
    private final String projectKey;
    private final String workspaceKey;
    private final Map<String, Object> params;
}

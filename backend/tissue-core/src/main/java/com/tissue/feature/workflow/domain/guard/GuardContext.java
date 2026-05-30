package com.tissue.feature.workflow.domain.guard;

import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.workflow.domain.WorkflowTransition;
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
    private final Map<String, Object> params;
}

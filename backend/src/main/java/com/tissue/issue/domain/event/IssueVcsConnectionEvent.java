package com.tissue.issue.domain.event;

import com.tissue.vcs.domain.enums.PrAction;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record IssueVcsConnectionEvent(
        String workspaceKey,
        String projectKey,
        String issueKey,
        Long issueId,
        String prTitle,
        String prUrl,
        PrAction prAction,
        String vcsUserEmail,
        String vcsUserName,
        LocalDateTime occurredAt) {}

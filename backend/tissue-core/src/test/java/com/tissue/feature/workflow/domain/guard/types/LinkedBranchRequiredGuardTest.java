package com.tissue.feature.workflow.domain.guard.types;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.IssueBranch;
import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.workflow.domain.exception.LinkedBranchRequiredException;
import com.tissue.feature.workflow.domain.guard.GuardContext;
import com.tissue.support.TestFixtures;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LinkedBranchRequiredGuardTest {

    private final LinkedBranchRequiredGuard guard = new LinkedBranchRequiredGuard();

    @Test
    @DisplayName("fail: an issue with no linked branch blocks the transition")
    void blocksWhenNoBranch() {
        // given
        Issue issue = TestFixtures.issue(TestFixtures.project("PROJ"), "test", IssueHierarchy.STANDARD);

        // when & then
        assertThatThrownBy(() -> guard.evaluate(context(issue))).isInstanceOf(LinkedBranchRequiredException.class);
    }

    @Test
    @DisplayName("success: an issue with at least one linked branch passes evaluation")
    void allowsWhenBranchLinked() {
        // given
        Project project = TestFixtures.project("PROJ");
        Issue issue = TestFixtures.issue(project, "test", IssueHierarchy.STANDARD);
        issue.addBranch(IssueBranch.create(
                issue, "https://example.com/repo.git", "feature/x", null, null, null, null, null, null));

        // when & then
        assertThatCode(() -> guard.evaluate(context(issue))).doesNotThrowAnyException();
    }

    private GuardContext context(Issue issue) {
        return GuardContext.builder()
                .issue(issue)
                .params(Map.of())
                .projectKey("PROJ")
                .build();
    }
}

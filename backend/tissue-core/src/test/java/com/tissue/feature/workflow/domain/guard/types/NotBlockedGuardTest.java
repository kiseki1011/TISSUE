package com.tissue.feature.workflow.domain.guard.types;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.feature.issue.domain.enums.IssueRelationType;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.workflow.domain.exception.IssueBlockedByDependencyException;
import com.tissue.feature.workflow.domain.guard.GuardContext;
import com.tissue.support.TestFixtures;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NotBlockedGuardTest {

    private final NotBlockedGuard guard = new NotBlockedGuard();

    @Test
    @DisplayName("fail: an unresolved blocking issue blocks the transition and reports its key")
    void blocksWhenBlockedByUnresolvedIssue() {
        // given - blocker BLOCKS issue
        Project project = TestFixtures.project("PROJ");
        Issue issue = TestFixtures.issue(project, "subject", IssueHierarchy.STANDARD);
        Issue blocker = TestFixtures.issue(project, "blocker", IssueHierarchy.STANDARD);
        blocker.addRelation(issue, IssueRelationType.BLOCKS);

        // when & then
        assertThatThrownBy(() -> guard.evaluate(context(issue)))
                .isInstanceOfSatisfying(IssueBlockedByDependencyException.class, ex -> assertThat(ex.getDetails())
                        .containsEntry("blockingIssueKeys", List.of(blocker.getKey())));
    }

    @Test
    @DisplayName("success: an issue with no blockers passes evaluation")
    void allowsWhenNoBlockers() {
        // given
        Issue issue = TestFixtures.issue(TestFixtures.project("PROJ"), "subject", IssueHierarchy.STANDARD);

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

package com.tissue.feature.workflow.domain.guard.types;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.workflow.domain.exception.AssigneeRequiredException;
import com.tissue.feature.workflow.domain.guard.GuardContext;
import com.tissue.support.TestFixtures;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AssigneeRequiredGuardTest {

    private final AssigneeRequiredGuard guard = new AssigneeRequiredGuard();

    @Test
    @DisplayName("fail: an issue with no assignee blocks the transition")
    void blocksWhenNoAssignee() {
        // given
        Issue issue = TestFixtures.issue(TestFixtures.project("PROJ"), "test", IssueHierarchy.STANDARD);

        // when & then
        assertThatThrownBy(() -> guard.evaluate(context(issue))).isInstanceOf(AssigneeRequiredException.class);
    }

    @Test
    @DisplayName("success: an assigned issue passes evaluation")
    void allowsWhenAssigned() {
        // given
        Project project = TestFixtures.project("PROJ");
        Issue issue = TestFixtures.issue(project, "test", IssueHierarchy.STANDARD);
        issue.assignTo(TestFixtures.projectMember(project, TestFixtures.member("doer")));

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

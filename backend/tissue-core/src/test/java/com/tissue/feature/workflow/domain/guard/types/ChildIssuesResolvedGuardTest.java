package com.tissue.feature.workflow.domain.guard.types;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.tissue.feature.issue.application.port.repository.IssueQueryRepository;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.feature.workflow.domain.exception.UnresolvedChildIssuesException;
import com.tissue.feature.workflow.domain.guard.GuardContext;
import com.tissue.support.TestFixtures;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChildIssuesResolvedGuardTest {

    @Mock
    private IssueQueryRepository issueQueryRepository;

    @Test
    @DisplayName("fail: unresolved child issues block the transition and report their keys")
    void blocksWhenChildrenUnresolved() {
        // given
        ChildIssuesResolvedGuard guard = new ChildIssuesResolvedGuard(issueQueryRepository);
        Issue issue = TestFixtures.issue(TestFixtures.project("PROJ"), "parent", IssueHierarchy.STANDARD);
        when(issueQueryRepository.findUnresolvedChildKeys(any(), any())).thenReturn(List.of("PROJ-3"));

        // when & then
        assertThatThrownBy(() -> guard.evaluate(context(issue)))
                .isInstanceOfSatisfying(UnresolvedChildIssuesException.class, ex -> assertThat(ex.getDetails())
                        .containsEntry("unresolvedChildKeys", List.of("PROJ-3")));
    }

    @Test
    @DisplayName("success: no unresolved children passes evaluation")
    void allowsWhenAllChildrenResolved() {
        // given
        ChildIssuesResolvedGuard guard = new ChildIssuesResolvedGuard(issueQueryRepository);
        Issue issue = TestFixtures.issue(TestFixtures.project("PROJ"), "parent", IssueHierarchy.STANDARD);
        when(issueQueryRepository.findUnresolvedChildKeys(any(), any())).thenReturn(List.of());

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

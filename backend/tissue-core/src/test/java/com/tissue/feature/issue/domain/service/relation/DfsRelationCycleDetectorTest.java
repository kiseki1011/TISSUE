package com.tissue.feature.issue.domain.service.relation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.feature.issue.domain.enums.IssueRelationType;
import com.tissue.feature.issue.domain.exception.RelationCycleDetectedException;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DfsRelationCycleDetectorTest {

    private final DfsRelationCycleDetector sut = new DfsRelationCycleDetector();

    @Nested
    @DisplayName("ensure no cycle")
    class EnsureNoCycle {

        @Test
        @DisplayName("success: no exception when no cycle exists")
        void successNoCycle() {
            // given
            Workspace ws = TestFixtures.workspace("WORKSPACE");
            Project project = TestFixtures.project(ws, "PROJ");
            Issue a = TestFixtures.issue(project, "A", IssueHierarchy.STANDARD);
            Issue b = TestFixtures.issue(project, "B", IssueHierarchy.STANDARD);
            Issue c = TestFixtures.issue(project, "C", IssueHierarchy.STANDARD);

            // when
            a.addRelation(b, IssueRelationType.BLOCKS);

            // then
            assertThatCode(() -> sut.ensureNoCycle(a, c, IssueRelationType.BLOCKS))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("success: skips acyclic check for 'RELEVANT' relation type")
        void successSkipCheckForRelevant() {
            // given
            Workspace ws = TestFixtures.workspace("WORKSPACE");
            Project project = TestFixtures.project(ws, "PROJ");
            Issue a = TestFixtures.issue(project, "A", IssueHierarchy.STANDARD);
            Issue b = TestFixtures.issue(project, "B", IssueHierarchy.STANDARD);
            Issue c = TestFixtures.issue(project, "C", IssueHierarchy.STANDARD);

            // when
            a.addRelation(b, IssueRelationType.BLOCKS);
            b.addRelation(c, IssueRelationType.BLOCKS);

            // then
            assertThatCode(() -> sut.ensureNoCycle(c, a, IssueRelationType.RELEVANT))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("fail: throws RelationCycleDetectedException when cycle exists")
        void failWhenCycleExists() {
            // given
            Workspace ws = TestFixtures.workspace("WORKSPACE");
            Project project = TestFixtures.project(ws, "PROJ");
            Issue a = TestFixtures.issue(project, "A", IssueHierarchy.STANDARD);
            Issue b = TestFixtures.issue(project, "B", IssueHierarchy.STANDARD);
            Issue c = TestFixtures.issue(project, "C", IssueHierarchy.STANDARD);

            // when
            a.addRelation(b, IssueRelationType.BLOCKS);
            b.addRelation(c, IssueRelationType.BLOCKS);

            // then
            assertThatThrownBy(() -> sut.ensureNoCycle(c, a, IssueRelationType.BLOCKS))
                    .isInstanceOf(RelationCycleDetectedException.class);
        }

        @Test
        @DisplayName("fail: detects cycle in longer chain (A → B → C → D → A)")
        void failWhenLongerChainCycleExists() {
            // given
            Workspace ws = TestFixtures.workspace("WORKSPACE");
            Project project = TestFixtures.project(ws, "PROJ");
            Issue a = TestFixtures.issue(project, "A", IssueHierarchy.STANDARD);
            Issue b = TestFixtures.issue(project, "B", IssueHierarchy.STANDARD);
            Issue c = TestFixtures.issue(project, "C", IssueHierarchy.STANDARD);
            Issue d = TestFixtures.issue(project, "D", IssueHierarchy.STANDARD);

            // when
            a.addRelation(b, IssueRelationType.BLOCKS);
            b.addRelation(c, IssueRelationType.BLOCKS);
            c.addRelation(d, IssueRelationType.BLOCKS);

            // then
            assertThatThrownBy(() -> sut.ensureNoCycle(d, a, IssueRelationType.BLOCKS))
                    .isInstanceOf(RelationCycleDetectedException.class);
        }
    }
}

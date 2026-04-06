package com.tissue.feature.issue.domain;

import static com.tissue.feature.issue.domain.exception.IssueErrorCode.ISSUE_SELF_REFERENCE;
import static com.tissue.feature.issue.domain.exception.IssueErrorCode.RELATION_ALREADY_EXISTS;
import static com.tissue.feature.issue.domain.exception.IssueErrorCode.RELATION_WORKSPACE_MISMATCH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.feature.issue.domain.enums.IssueRelationType;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.shared.exception.base.BadRequestException;
import com.tissue.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class IssueRelationTest {

    @Nested
    @DisplayName("add relation")
    class AddRelation {

        @Test
        @DisplayName("success: relation is added on both outgoing and incoming")
        void successAddRelation() {
            // given
            Workspace ws = TestFixtures.workspace("WORKSPACE");
            Project project = TestFixtures.project(ws, "PROJ");
            Issue sourceIssue = TestFixtures.issue(project, "source issue", IssueHierarchy.STANDARD);
            Issue targetIssue = TestFixtures.issue(project, "target issue", IssueHierarchy.STANDARD);

            // when
            IssueRelation relation = sourceIssue.addRelation(targetIssue, IssueRelationType.BLOCKS);

            // then
            assertThat(relation.getRelationType()).isEqualTo(IssueRelationType.BLOCKS);
            assertThat(sourceIssue.getRelations().getOutgoingRelations()).containsExactly(relation);
            assertThat(targetIssue.getRelations().getIncomingRelations()).containsExactly(relation);
        }

        @Test
        @DisplayName("fail: throws BadRequestException if self-reference")
        void failAddRelation_If_SelfReference() {
            // given
            Workspace ws = TestFixtures.workspace("WORKSPACE");
            Project project = TestFixtures.project(ws, "PROJ");
            Issue sourceIssue = TestFixtures.issue(project, "source issue", IssueHierarchy.STANDARD);

            // when & then
            assertThatThrownBy(() -> sourceIssue.addRelation(sourceIssue, IssueRelationType.BLOCKS))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("errorCode")
                    .isEqualTo(ISSUE_SELF_REFERENCE);
        }

        @Test
        @DisplayName("fail: throws BadRequestException if the two issues are not in the same workspace")
        void failAddRelation_If_CrossWorkspace() {
            // given
            Workspace ws = TestFixtures.workspace("WORKSPACE");
            Workspace ws2 = TestFixtures.workspace("WORKSPACE2");
            Project project = TestFixtures.project(ws, "PROJ");
            Project project2 = TestFixtures.project(ws2, "PROJ2");
            Issue sourceIssue = TestFixtures.issue(project, "source issue", IssueHierarchy.STANDARD);
            Issue targetIssue = TestFixtures.issue(project2, "target issue", IssueHierarchy.STANDARD);

            // when & then
            assertThatThrownBy(() -> sourceIssue.addRelation(targetIssue, IssueRelationType.BLOCKS))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("errorCode")
                    .isEqualTo(RELATION_WORKSPACE_MISMATCH);
        }

        @Test
        @DisplayName("fail: throws BadRequestException if relation already exists")
        void failAddRelation_If_RelationExists() {
            // given
            Workspace ws = TestFixtures.workspace("WORKSPACE");
            Project project = TestFixtures.project(ws, "PROJ");
            Issue sourceIssue = TestFixtures.issue(project, "source issue", IssueHierarchy.STANDARD);
            Issue targetIssue = TestFixtures.issue(project, "target issue", IssueHierarchy.STANDARD);

            sourceIssue.addRelation(targetIssue, IssueRelationType.BLOCKS);

            // when & then
            assertThatThrownBy(() -> sourceIssue.addRelation(targetIssue, IssueRelationType.RELEVANT))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("errorCode")
                    .isEqualTo(RELATION_ALREADY_EXISTS);
        }
    }
}

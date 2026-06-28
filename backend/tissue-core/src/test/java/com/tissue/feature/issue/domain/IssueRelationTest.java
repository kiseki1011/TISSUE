package com.tissue.feature.issue.domain;

import static com.tissue.feature.issue.domain.exception.IssueErrorCode.ISSUE_SELF_REFERENCE;
import static com.tissue.feature.issue.domain.exception.IssueErrorCode.RELATION_ALREADY_EXISTS;
import static com.tissue.feature.issue.domain.exception.IssueErrorCode.RELATION_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.feature.issue.domain.enums.IssueRelationType;
import com.tissue.feature.project.domain.Project;
import com.tissue.shared.exception.base.BadRequestException;
import com.tissue.shared.exception.base.ResourceNotFoundException;
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
            Project project = TestFixtures.project("PROJ");
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
            Project project = TestFixtures.project("PROJ");
            Issue sourceIssue = TestFixtures.issue(project, "source issue", IssueHierarchy.STANDARD);

            // when & then
            assertThatThrownBy(() -> sourceIssue.addRelation(sourceIssue, IssueRelationType.BLOCKS))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("errorCode")
                    .isEqualTo(ISSUE_SELF_REFERENCE);
        }

        @Test
        @DisplayName("success: relation across different projects is allowed")
        void successAddRelation_AcrossProjects() {
            // given
            Project project = TestFixtures.project("PROJ");
            Project project2 = TestFixtures.project("PROJ2");
            Issue sourceIssue = TestFixtures.issue(project, "source issue", IssueHierarchy.STANDARD);
            Issue targetIssue = TestFixtures.issue(project2, "target issue", IssueHierarchy.STANDARD);

            // when
            IssueRelation relation = sourceIssue.addRelation(targetIssue, IssueRelationType.BLOCKS);

            // then
            assertThat(relation.getRelationType()).isEqualTo(IssueRelationType.BLOCKS);
            assertThat(sourceIssue.getRelations().getOutgoingRelations()).containsExactly(relation);
        }

        @Test
        @DisplayName("fail: throws BadRequestException if relation already exists")
        void failAddRelation_If_RelationExists() {
            // given
            Project project = TestFixtures.project("PROJ");
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

    @Nested
    @DisplayName("remove relation")
    class RemoveRelation {

        @Test
        @DisplayName("success: a RELEVANT relation can be removed from the target side")
        void successRemoveRelevant_FromTargetSide() {
            // given
            Project project = TestFixtures.project("PROJ");
            Issue source = TestFixtures.issue(project, "source issue", IssueHierarchy.STANDARD);
            Issue target = TestFixtures.issue(project, "target issue", IssueHierarchy.STANDARD);
            source.addRelation(target, IssueRelationType.RELEVANT);

            // when: removed from the target side (target is not the relation's source)
            target.removeRelation(source);

            // then
            assertThat(source.getRelations().getOutgoingRelations()).isEmpty();
            assertThat(target.getRelations().getIncomingRelations()).isEmpty();
        }

        @Test
        @DisplayName("fail: a directional BLOCKS relation cannot be removed from the target side")
        void failRemoveBlocks_FromTargetSide() {
            // given
            Project project = TestFixtures.project("PROJ");
            Issue source = TestFixtures.issue(project, "source issue", IssueHierarchy.STANDARD);
            Issue target = TestFixtures.issue(project, "target issue", IssueHierarchy.STANDARD);
            source.addRelation(target, IssueRelationType.BLOCKS);

            // when & then: only the source may remove a directional relation
            assertThatThrownBy(() -> target.removeRelation(source))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .extracting("errorCode")
                    .isEqualTo(RELATION_NOT_FOUND);
            assertThat(source.getRelations().getOutgoingRelations()).hasSize(1);
        }

        @Test
        @DisplayName("success: a BLOCKS relation is removed from the source side")
        void successRemoveBlocks_FromSourceSide() {
            // given
            Project project = TestFixtures.project("PROJ");
            Issue source = TestFixtures.issue(project, "source issue", IssueHierarchy.STANDARD);
            Issue target = TestFixtures.issue(project, "target issue", IssueHierarchy.STANDARD);
            source.addRelation(target, IssueRelationType.BLOCKS);

            // when
            source.removeRelation(target);

            // then
            assertThat(source.getRelations().getOutgoingRelations()).isEmpty();
            assertThat(target.getRelations().getIncomingRelations()).isEmpty();
        }
    }
}

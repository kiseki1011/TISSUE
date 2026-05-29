package com.tissue.feature.issue.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.tissue.feature.issue.application.service.finder.IssueFinder;
import com.tissue.feature.issue.application.service.publisher.IssueEventPublisher;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.IssueRelation;
import com.tissue.feature.issue.domain.enums.IssueRelationType;
import com.tissue.feature.issue.domain.exception.RelationCycleDetectedException;
import com.tissue.feature.issue.domain.service.relation.RelationCycleDetector;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.shared.dto.IssueIdentifier;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IssueRelationServiceTest {

    @Mock
    private IssueFinder issueFinder;

    @Mock
    private ProjectMemberFinder projectMemberFinder;

    @Mock
    private RelationCycleDetector relationCycleDetector;

    @Mock
    private IssueEventPublisher eventPublisher;

    @InjectMocks
    private IssueRelationService sut;

    @Nested
    @DisplayName("add issue relation")
    class AddIssueRelation {

        @Test
        @DisplayName("success: success adding issue relation")
        void successAddIssueRelation() {
            // given
            IssueIdentifier sourceIid = new IssueIdentifier("PROJ", "PROJ-1");
            String targetIssueKey = "PROJ-2";
            Long actorMemberId = 1L;

            ProjectMember actor = mock(ProjectMember.class);
            Issue sourceIssue = mock(Issue.class);
            Issue targetIssue = mock(Issue.class);
            IssueRelation relation = mock(IssueRelation.class);

            given(projectMemberFinder.getByProjectKey(sourceIid.projectKey(), actorMemberId))
                    .willReturn(actor);
            given(issueFinder.getWithProjectByIssueKey(sourceIid.issueKey())).willReturn(sourceIssue);
            given(issueFinder.getWithProjectByIssueKey(targetIssueKey)).willReturn(targetIssue);
            given(sourceIssue.addRelation(targetIssue, IssueRelationType.BLOCKS))
                    .willReturn(relation);

            // when
            sut.add(sourceIid, targetIssueKey, IssueRelationType.BLOCKS, actorMemberId);

            // then
            then(relationCycleDetector).should().ensureNoCycle(sourceIssue, targetIssue, IssueRelationType.BLOCKS);
            then(sourceIssue).should().addRelation(targetIssue, IssueRelationType.BLOCKS);
            then(eventPublisher).should().publishRelationAdded(sourceIssue, targetIssue, relation, actor);
        }

        @Test
        @DisplayName("fail: throws RelationCycleDetectedException if cycle is detected when adding relation")
        void fail_If_CycleDetected() {
            // given
            IssueIdentifier sourceIid = new IssueIdentifier("PROJ", "PROJ-1");
            String targetIssueKey = "PROJ-2";
            Long actorMemberId = 1L;

            ProjectMember actor = mock(ProjectMember.class);
            Issue sourceIssue = mock(Issue.class);
            Issue targetIssue = mock(Issue.class);

            given(projectMemberFinder.getByProjectKey(sourceIid.projectKey(), actorMemberId))
                    .willReturn(actor);
            given(issueFinder.getWithProjectByIssueKey(sourceIid.issueKey())).willReturn(sourceIssue);
            given(issueFinder.getWithProjectByIssueKey(targetIssueKey)).willReturn(targetIssue);

            willThrow(new RelationCycleDetectedException("PROJ-1", "PROJ-2", IssueRelationType.BLOCKS, List.of()))
                    .given(relationCycleDetector)
                    .ensureNoCycle(sourceIssue, targetIssue, IssueRelationType.BLOCKS);

            // when & then
            assertThatThrownBy(() -> sut.add(sourceIid, targetIssueKey, IssueRelationType.BLOCKS, actorMemberId))
                    .isInstanceOf(RelationCycleDetectedException.class);

            then(sourceIssue).should(never()).addRelation(targetIssue, IssueRelationType.BLOCKS);
            then(eventPublisher).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("remove issue relation")
    class RemoveIssueRelation {

        @Test
        @DisplayName("success: success removing issue relation")
        void successRemoveIssueRelation() {
            // given
            IssueIdentifier sourceIid = new IssueIdentifier("PROJ", "PROJ-1");
            String targetIssueKey = "PROJ-2";
            Long actorMemberId = 1L;

            ProjectMember actor = mock(ProjectMember.class);
            Issue sourceIssue = mock(Issue.class);
            Issue targetIssue = mock(Issue.class);
            IssueRelation removedRelation = mock(IssueRelation.class);

            given(projectMemberFinder.getByProjectKey(sourceIid.projectKey(), actorMemberId))
                    .willReturn(actor);
            given(issueFinder.getWithProjectByIssueKey(sourceIid.issueKey())).willReturn(sourceIssue);
            given(issueFinder.getWithProjectByIssueKey(targetIssueKey)).willReturn(targetIssue);
            given(sourceIssue.removeRelation(targetIssue)).willReturn(removedRelation);

            // when
            sut.remove(sourceIid, targetIssueKey, actorMemberId);

            // then
            then(sourceIssue).should().removeRelation(targetIssue);
            then(eventPublisher).should().publishRelationRemoved(sourceIssue, targetIssue, removedRelation, actor);
        }
    }
}

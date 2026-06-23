package com.tissue.feature.issue.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.tissue.feature.issue.application.dto.request.BatchChangeParentCommand;
import com.tissue.feature.issue.application.dto.request.BatchRemoveParentCommand;
import com.tissue.feature.issue.application.dto.request.UpdateCommonFieldsCommand;
import com.tissue.feature.issue.application.service.finder.IssueFinder;
import com.tissue.feature.issue.application.service.publisher.IssueEventPublisher;
import com.tissue.feature.issue.application.service.validator.CustomFieldSchemaProcessor;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.service.IssueFieldChangeTracker;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.shared.dto.BatchOperationResponse;
import com.tissue.shared.dto.FieldChange;
import com.tissue.shared.dto.IssueIdentifier;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.exception.ErrorCode;
import com.tissue.shared.exception.base.BadRequestException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.jackson.nullable.JsonNullable;

@ExtendWith(MockitoExtension.class)
class IssueUpdateServiceTest {

    @Mock
    private IssueFinder issueFinder;

    @Mock
    private ProjectMemberFinder projectMemberFinder;

    @Mock
    private CustomFieldSchemaProcessor customFieldSchemaProcessor;

    @Mock
    private IssueFieldChangeTracker fieldChangeTracker;

    @Mock
    private IssueEventPublisher eventPublisher;

    @InjectMocks
    private IssueUpdateService sut;

    @Nested
    @DisplayName("update common fields")
    class UpdateCommonFields {

        @Test
        @DisplayName("success: publishes event when fields actually change")
        void successPublishesEvent_WhenFieldsChange() {
            // given
            IssueIdentifier iid = new IssueIdentifier("PROJ", "PROJ-1");
            Long actorMemberId = 1L;

            UpdateCommonFieldsCommand cmd = UpdateCommonFieldsCommand.builder()
                    .title(JsonNullable.of("new title"))
                    .content(JsonNullable.undefined())
                    .summary(JsonNullable.undefined())
                    .priority(JsonNullable.undefined())
                    .dueAt(JsonNullable.undefined())
                    .build();

            ProjectMember actor = mock(ProjectMember.class);
            Issue issue = mock(Issue.class);

            given(projectMemberFinder.getByProjectKey(iid.projectKey(), actorMemberId))
                    .willReturn(actor);
            given(issueFinder.getWithProjectByIssueKey(iid.issueKey())).willReturn(issue);

            // when
            sut.updateCommonFields(iid, cmd, actorMemberId);

            // then
            then(issue).should().updateTitle("new title");
            then(eventPublisher).should().publishIssueFieldsUpdated(eq(issue), any(), eq(actor));
        }

        @Test
        @DisplayName("success: does not publish event when no fields change")
        void successNoEvent_WhenNoFieldsChange() {
            // given
            IssueIdentifier iid = new IssueIdentifier("PROJ", "PROJ-1");
            Long actorMemberId = 1L;

            UpdateCommonFieldsCommand cmd = UpdateCommonFieldsCommand.builder()
                    .title(JsonNullable.undefined())
                    .content(JsonNullable.undefined())
                    .summary(JsonNullable.undefined())
                    .priority(JsonNullable.undefined())
                    .dueAt(JsonNullable.undefined())
                    .build();

            ProjectMember actor = mock(ProjectMember.class);
            Issue issue = mock(Issue.class);

            given(projectMemberFinder.getByProjectKey(iid.projectKey(), actorMemberId))
                    .willReturn(actor);
            given(issueFinder.getWithProjectByIssueKey(iid.issueKey())).willReturn(issue);

            // when
            sut.updateCommonFields(iid, cmd, actorMemberId);

            // then
            then(eventPublisher).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("update custom fields")
    class UpdateCustomFields {

        @Test
        @DisplayName("success: publishes event when custom fields change")
        void successPublishesEvent_WhenCustomFieldsChange() {
            // given
            IssueIdentifier iid = new IssueIdentifier("PROJ", "PROJ-1");
            Long actorMemberId = 1L;
            Map<Long, Object> customFields = new HashMap<>(Map.of(1L, "new value"));

            ProjectMember actor = mock(ProjectMember.class);
            Issue issue = mock(Issue.class);
            IssueType issueType = mock(IssueType.class);

            given(projectMemberFinder.getByProjectKey(iid.projectKey(), actorMemberId))
                    .willReturn(actor);
            given(issueFinder.getWithProjectIssueTypeAndFieldsByIssueKey(iid.issueKey()))
                    .willReturn(issue);
            given(issue.getIssueType()).willReturn(issueType);
            given(issueType.getFields()).willReturn(List.of());

            Map<String, Object> oldSnapshot = Map.of("1", "old value");
            Map<String, Object> newSnapshot = Map.of("1", "new value");
            given(fieldChangeTracker.captureSnapshot(issue)).willReturn(oldSnapshot, newSnapshot);

            Map<String, FieldChange> changes = Map.of("1", FieldChange.of("old value", "new value"));
            given(fieldChangeTracker.compareChanges(oldSnapshot, newSnapshot, Map.of()))
                    .willReturn(changes);

            // when
            sut.updateCustomFields(iid, customFields, actorMemberId);

            // then
            then(customFieldSchemaProcessor).should().validateAndApplyPatch(customFields, issue);
            then(eventPublisher).should().publishIssueFieldsUpdated(issue, changes, actor);
        }

        @Test
        @DisplayName("success: does not publish event when no custom fields change")
        void successNoEvent_WhenNoCustomFieldsChange() {
            // given
            IssueIdentifier iid = new IssueIdentifier("PROJ", "PROJ-1");
            Long actorMemberId = 1L;
            Map<Long, Object> customFields = new HashMap<>(Map.of(1L, "same"));

            ProjectMember actor = mock(ProjectMember.class);
            Issue issue = mock(Issue.class);
            IssueType issueType = mock(IssueType.class);

            given(projectMemberFinder.getByProjectKey(iid.projectKey(), actorMemberId))
                    .willReturn(actor);
            given(issueFinder.getWithProjectIssueTypeAndFieldsByIssueKey(iid.issueKey()))
                    .willReturn(issue);
            given(issue.getIssueType()).willReturn(issueType);
            given(issueType.getFields()).willReturn(List.of());

            Map<String, Object> snapshot = Map.of("1", "same");
            given(fieldChangeTracker.captureSnapshot(issue)).willReturn(snapshot);
            given(fieldChangeTracker.compareChanges(snapshot, snapshot, Map.of()))
                    .willReturn(Map.of());

            // when
            sut.updateCustomFields(iid, customFields, actorMemberId);

            // then
            then(customFieldSchemaProcessor).should().validateAndApplyPatch(customFields, issue);
            then(eventPublisher).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("remove parent")
    class RemoveParent {

        @Test
        @DisplayName("success: removes parent and publishes event")
        void successRemoveParent() {
            // given
            IssueIdentifier iid = new IssueIdentifier("PROJ", "PROJ-1");
            Long actorMemberId = 1L;

            ProjectMember actor = mock(ProjectMember.class);
            Issue issue = mock(Issue.class);
            Issue parent = mock(Issue.class);

            given(projectMemberFinder.getByProjectKey(iid.projectKey(), actorMemberId))
                    .willReturn(actor);
            given(issueFinder.getWithProjectByIssueKey(iid.issueKey())).willReturn(issue);
            given(issue.getParentIssue()).willReturn(parent);

            // when
            sut.removeParent(iid, actorMemberId);

            // then
            then(issue).should().removeParentIssue();
            then(eventPublisher).should().publishParentChanged(issue, parent, null, actor);
        }

        @Test
        @DisplayName("success: early-return when parent is already null")
        void successEarlyReturn_If_ParentNull() {
            // given
            IssueIdentifier iid = new IssueIdentifier("PROJ", "PROJ-1");
            Long actorMemberId = 1L;

            ProjectMember actor = mock(ProjectMember.class);
            Issue issue = mock(Issue.class);

            given(projectMemberFinder.getByProjectKey(iid.projectKey(), actorMemberId))
                    .willReturn(actor);
            given(issueFinder.getWithProjectByIssueKey(iid.issueKey())).willReturn(issue);
            given(issue.getParentIssue()).willReturn(null);

            // when
            sut.removeParent(iid, actorMemberId);

            // then
            then(issue).should(never()).removeParentIssue();
            then(eventPublisher).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("batch assign parent")
    class BatchAssignParent {

        @Test
        @DisplayName("success: partial failure collects failed issue keys")
        void successPartialFailure() {
            // given
            ProjectIdentifier pid = new ProjectIdentifier("PROJ");
            Long actorMemberId = 1L;
            BatchChangeParentCommand cmd = new BatchChangeParentCommand(Set.of("PROJ-1", "PROJ-2"), "PROJ-100");

            ProjectMember actor = mock(ProjectMember.class);
            Issue issue1 = mock(Issue.class);
            Issue issue2 = mock(Issue.class);
            Issue newParent = mock(Issue.class);

            given(projectMemberFinder.getByProjectKey(pid.projectKey(), actorMemberId))
                    .willReturn(actor);
            given(issueFinder.getAllByIssueKeys(cmd.issueKeys())).willReturn(List.of(issue1, issue2));
            given(issueFinder.getWithProjectByIssueKey(cmd.parentIssueKey())).willReturn(newParent);

            given(issue1.getKey()).willReturn("PROJ-1");
            willThrow(new BadRequestException(mock(ErrorCode.class)))
                    .given(issue1)
                    .setParentIssue(newParent);

            // when
            BatchOperationResponse result = sut.batchAssignParent(pid, cmd, actorMemberId);

            // then
            assertThat(result.totalCount()).isEqualTo(2);
            assertThat(result.successCount()).isEqualTo(1);
            assertThat(result.failures()).hasSize(1);
            assertThat(result.failures().getFirst().key()).isEqualTo("PROJ-1");
            then(issue2).should().setParentIssue(newParent);
        }
    }

    @Nested
    @DisplayName("batch remove parent")
    class BatchRemoveParent {

        @Test
        @DisplayName("success: skips issues without parent and removes others")
        void successSkipsNullParent() {
            // given
            ProjectIdentifier pid = new ProjectIdentifier("PROJ");
            Long actorMemberId = 1L;
            BatchRemoveParentCommand cmd = new BatchRemoveParentCommand(Set.of("PROJ-1", "PROJ-2"));

            ProjectMember actor = mock(ProjectMember.class);
            Issue issue1 = mock(Issue.class);
            Issue issue2 = mock(Issue.class);
            Issue parent2 = mock(Issue.class);

            given(projectMemberFinder.getByProjectKey(pid.projectKey(), actorMemberId))
                    .willReturn(actor);
            given(issueFinder.getAllByIssueKeys(cmd.issueKeys())).willReturn(List.of(issue1, issue2));

            given(issue1.getParentIssue()).willReturn(null);
            given(issue2.getParentIssue()).willReturn(parent2);

            // when
            BatchOperationResponse result = sut.batchRemoveParent(pid, cmd, actorMemberId);

            // then
            assertThat(result.totalCount()).isEqualTo(2);
            assertThat(result.successCount()).isEqualTo(2);
            assertThat(result.failures()).isEmpty();
            then(issue1).should(never()).removeParentIssue();
            then(issue2).should().removeParentIssue();
            then(eventPublisher).should().publishParentChanged(issue2, parent2, null, actor);
        }
    }
}

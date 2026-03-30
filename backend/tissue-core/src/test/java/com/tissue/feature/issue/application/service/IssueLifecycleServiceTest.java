package com.tissue.feature.issue.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.tissue.feature.issue.application.dto.request.BatchSoftDeleteCommand;
import com.tissue.feature.issue.application.dto.request.CreateIssueCommand;
import com.tissue.feature.issue.application.port.repository.IssueCommandRepository;
import com.tissue.feature.issue.application.service.authorization.IssueAuthorizationService;
import com.tissue.feature.issue.application.service.finder.IssueFinder;
import com.tissue.feature.issue.application.service.publisher.IssueEventPublisher;
import com.tissue.feature.issue.application.service.validator.CustomFieldSchemaProcessor;
import com.tissue.feature.issue.application.service.validator.IssueValidator;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.feature.issue.domain.enums.IssuePriority;
import com.tissue.feature.issuetype.application.service.finder.IssueTypeFinder;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.project.application.service.finder.ProjectFinder;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.sprint.application.service.SprintFinder;
import com.tissue.feature.sprint.domain.Sprint;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.feature.workflow.domain.WorkflowState;
import com.tissue.shared.dto.BatchOperationResponse;
import com.tissue.shared.dto.IssueIdentifier;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.exception.ErrorCode;
import com.tissue.shared.exception.base.ForbiddenException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

@ExtendWith(MockitoExtension.class)
class IssueLifecycleServiceTest {

    @Mock
    private IssueFinder issueFinder;

    @Mock
    private IssueTypeFinder issueTypeFinder;

    @Mock
    private SprintFinder sprintFinder;

    @Mock
    private ProjectFinder projectFinder;

    @Mock
    private ProjectMemberFinder projectMemberFinder;

    @Mock
    private CustomFieldSchemaProcessor fieldSchemaValidator;

    @Mock
    private IssueValidator issueValidator;

    @Mock
    private IssueCommandRepository issueCommandRepository;

    @Mock
    private IssueAuthorizationService issueAuthorizationService;

    @Mock
    private IssueEventPublisher eventPublisher;

    @InjectMocks
    private IssueLifecycleService sut;

    @Nested
    @DisplayName("create issue")
    class CreateIssue {

        @Test
        @DisplayName("success: create issue with all fields filled")
        void successCreateIssueWithAllFields() {
            // given
            ProjectIdentifier pid = new ProjectIdentifier("WORKSPACE", "PROJ");
            Long actorMemberId = 1L;
            Long assigneeMemberId = 2L;
            Long sprintId = 30L;
            String parentKey = "PROJ-123";
            Long issueTypeId = 400L;
            Map<Long, Object> customFields = new HashMap<>(Map.of(1L, "field1", 2L, "field2"));

            CreateIssueCommand cmd = CreateIssueCommand.builder()
                    .sprintId(sprintId)
                    .parentProjectKey(pid.projectKey())
                    .parentKey(parentKey)
                    .title("full issue")
                    .content("full issue content")
                    .summary("full issue summary")
                    .priority(IssuePriority.NORMAL)
                    .dueAt(Instant.now().plus(1, ChronoUnit.DAYS))
                    .storyPoint(10)
                    .issueTypeId(issueTypeId)
                    .customFields(customFields)
                    .assigneeMemberId(assigneeMemberId)
                    .build();

            ProjectMember actor = mock(ProjectMember.class);
            ProjectMember assignee = mock(ProjectMember.class);
            IssueType issueType = mock(IssueType.class);
            Project project = mock(Project.class);
            Sprint sprint = mock(Sprint.class);
            Issue parent = mock(Issue.class);

            given(projectMemberFinder.getWithWorkspaceMember(pid.workspaceKey(), pid.projectKey(), actorMemberId))
                    .willReturn(actor);

            given(issueTypeFinder.getWithProjectAndWorkflowBy(pid.workspaceKey(), pid.projectKey(), issueTypeId))
                    .willReturn(issueType);

            Workflow mockWorkflow = mock(Workflow.class);
            WorkflowState mockInitialState = mock(WorkflowState.class);
            given(issueType.getWorkflow()).willReturn(mockWorkflow);
            given(mockWorkflow.getInitialState()).willReturn(mockInitialState);
            given(issueType.getIssueHierarchy()).willReturn(IssueHierarchy.STANDARD);

            given(projectFinder.getWithLockBy(pid.workspaceKey(), pid.projectKey()))
                    .willReturn(project);

            given(sprintFinder.getBy(sprintId, project)).willReturn(sprint);
            given(issueFinder.getWithProjectBy(pid.workspaceKey(), parentKey)).willReturn(parent);
            given(parent.getHierarchy()).willReturn(IssueHierarchy.EPIC);
            given(projectMemberFinder.getBy(project, cmd.assigneeMemberId())).willReturn(assignee);

            // when
            sut.create(pid, cmd, actorMemberId);

            // then
            then(fieldSchemaValidator).should().validateAndAssign(eq(customFields), any(Issue.class));
            then(issueCommandRepository).should().save(any(Issue.class));
            then(eventPublisher).should().publishIssueCreated(any(Issue.class), eq(actor));
        }

        @Test
        @DisplayName("success: create issue with nullable fields as null")
        void successCreateIssueWithNullableFieldsNull() {
            // given
            ProjectIdentifier pid = new ProjectIdentifier("WORKSPACE", "PROJ");
            Long actorMemberId = 1L;
            Long issueTypeId = 400L;

            CreateIssueCommand cmd = CreateIssueCommand.builder()
                    .sprintId(null)
                    .parentKey(null)
                    .title("minimal issue")
                    .priority(IssuePriority.NORMAL)
                    .issueTypeId(issueTypeId)
                    .assigneeMemberId(null)
                    .build();

            ProjectMember actor = mock(ProjectMember.class);
            IssueType issueType = mock(IssueType.class);
            Project project = mock(Project.class);
            Workflow mockWorkflow = mock(Workflow.class);
            WorkflowState mockInitialState = mock(WorkflowState.class);

            given(projectMemberFinder.getWithWorkspaceMember(pid.workspaceKey(), pid.projectKey(), actorMemberId))
                    .willReturn(actor);
            given(issueTypeFinder.getWithProjectAndWorkflowBy(pid.workspaceKey(), pid.projectKey(), issueTypeId))
                    .willReturn(issueType);
            given(issueType.getWorkflow()).willReturn(mockWorkflow);
            given(mockWorkflow.getInitialState()).willReturn(mockInitialState);
            given(projectFinder.getWithLockBy(pid.workspaceKey(), pid.projectKey()))
                    .willReturn(project);

            // when
            sut.create(pid, cmd, actorMemberId);

            // then
            then(sprintFinder).shouldHaveNoInteractions();
            then(issueFinder).shouldHaveNoInteractions();
            then(issueCommandRepository).should().save(any(Issue.class));
        }
    }

    @Nested
    @DisplayName("soft delete issue")
    class SoftDeleteIssue {

        @Test
        @DisplayName("success: deletes issue after authorization and validation")
        void successSoftDeleteIssue() {
            // given
            IssueIdentifier iid = IssueIdentifier.of("WORKSPACE", "PROJ", "PROJ-1");
            Long actorMemberId = 1L;

            ProjectMember actor = mock(ProjectMember.class);
            Issue issue = mock(Issue.class);

            given(projectMemberFinder.getWithWorkspaceMember(iid.workspaceKey(), iid.projectKey(), actorMemberId))
                    .willReturn(actor);
            given(issueFinder.getWithProjectBy(iid.workspaceKey(), iid.issueKey()))
                    .willReturn(issue);

            // when
            sut.delete(iid, actorMemberId);

            // then
            then(issueAuthorizationService).should().requireIssueDeletePermission(issue, actor);
            then(issueValidator).should().ensureCanDelete(issue);
            then(issue).should().delete();
            then(eventPublisher).should().publishIssueDeleted(issue, actor);
        }
    }

    @Nested
    @DisplayName("restore issue")
    class RestoreIssue {

        @Test
        @DisplayName("success: restores soft deleted issue")
        void successRestoreIssue() {
            // given
            IssueIdentifier iid = IssueIdentifier.of("WORKSPACE", "PROJ", "PROJ-1");
            Long actorMemberId = 1L;

            ProjectMember actor = mock(ProjectMember.class);
            Issue issue = mock(Issue.class);

            given(projectMemberFinder.getWithWorkspaceMember(iid.workspaceKey(), iid.projectKey(), actorMemberId))
                    .willReturn(actor);
            given(issueFinder.getDeletedWithProjectBy(iid.workspaceKey(), iid.issueKey()))
                    .willReturn(issue);

            // when
            sut.restore(iid, actorMemberId);

            // then
            then(issueAuthorizationService).should().requireIssueDeletePermission(issue, actor);
            then(issue).should().restoreSoftDeleted();
            then(eventPublisher).should().publishIssueRestored(issue, actor);
        }
    }

    @Nested
    @DisplayName("batch soft delete issue")
    class BatchSoftDeleteIssue {

        @Test
        @DisplayName("success: full success returns empty failures with exact total count")
        void successBatchSoftDelete_With_FullSuccess() {
            // given
            ProjectIdentifier pid = new ProjectIdentifier("WORKSPACE", "PROJ");
            Long actorMemberId = 1L;
            BatchSoftDeleteCommand cmd = new BatchSoftDeleteCommand(Set.of("PROJ-1", "PROJ-2"));

            ProjectMember actor = mock(ProjectMember.class);
            Issue issue1 = mock(Issue.class);
            Issue issue2 = mock(Issue.class);

            given(projectMemberFinder.getWithWorkspaceMember(pid.workspaceKey(), pid.projectKey(), actorMemberId))
                    .willReturn(actor);
            given(issueFinder.getAllBy(cmd.issueKeys(), pid.workspaceKey())).willReturn(List.of(issue1, issue2));

            // when
            BatchOperationResponse result = sut.batchSoftDelete(pid, cmd, actorMemberId);

            // then
            assertThat(result.totalCount()).isEqualTo(2);
            assertThat(result.successCount()).isEqualTo(2);
            assertThat(result.failures()).isEmpty();
            then(issue1).should().delete();
            then(issue2).should().delete();
        }

        @Test
        @DisplayName("success: partial failure collects failed issue keys and continues")
        void successBatchSoftDelete_With_PartialSuccess() {
            // given
            ProjectIdentifier pid = new ProjectIdentifier("WORKSPACE", "PROJ");
            Long actorMemberId = 1L;
            BatchSoftDeleteCommand cmd = new BatchSoftDeleteCommand(Set.of("PROJ-1", "PROJ-2"));

            ProjectMember actor = mock(ProjectMember.class);
            Issue issue1 = mock(Issue.class);
            Issue issue2 = mock(Issue.class);

            given(projectMemberFinder.getWithWorkspaceMember(pid.workspaceKey(), pid.projectKey(), actorMemberId))
                    .willReturn(actor);
            given(issueFinder.getAllBy(cmd.issueKeys(), pid.workspaceKey())).willReturn(List.of(issue1, issue2));

            given(issue1.getKey()).willReturn("PROJ-1");
            willThrow(new ForbiddenException(mock(ErrorCode.class)))
                    .given(issueAuthorizationService)
                    .requireIssueDeletePermission(issue1, actor);

            // when
            BatchOperationResponse result = sut.batchSoftDelete(pid, cmd, actorMemberId);

            // then
            assertThat(result.totalCount()).isEqualTo(2);
            assertThat(result.successCount()).isEqualTo(1);
            assertThat(result.failCount()).isEqualTo(1);
            assertThat(result.failures()).hasSize(1);
            assertThat(result.failures().getFirst().key()).isEqualTo("PROJ-1");
            then(issue1).should(never()).delete();
            then(issue2).should().delete();
        }
    }
}

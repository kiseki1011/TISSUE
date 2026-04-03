package com.tissue.feature.issuetype.application.service;

import static com.tissue.feature.issuetype.domain.exception.IssueTypeErrorCode.DUPLICATE_ISSUE_TYPE_NAME;
import static com.tissue.feature.issuetype.domain.exception.IssueTypeErrorCode.ISSUE_TYPE_IN_USE;
import static com.tissue.feature.project.domain.exception.ProjectErrorCode.PROJECT_MANAGER_REQUIRED;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.feature.issuetype.application.dto.request.CreateIssueTypeCommand;
import com.tissue.feature.issuetype.application.dto.request.PatchIssueTypeCommand;
import com.tissue.feature.issuetype.application.port.repository.IssueTypeRepository;
import com.tissue.feature.issuetype.application.service.finder.IssueTypeFinder;
import com.tissue.feature.issuetype.application.service.validator.IssueTypeValidator;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.workflow.application.service.finder.WorkflowFinder;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.enums.IconType;
import com.tissue.shared.exception.base.BadRequestException;
import com.tissue.shared.exception.base.ForbiddenException;
import com.tissue.shared.exception.base.ResourceConflictException;
import com.tissue.shared.vo.Name;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.jackson.nullable.JsonNullable;

@ExtendWith(MockitoExtension.class)
class IssueTypeServiceTest {

    @Mock
    private WorkflowFinder workflowFinder;

    @Mock
    private IssueTypeFinder issueTypeFinder;

    @Mock
    private ProjectMemberFinder projectMemberFinder;

    @Mock
    private IssueTypeRepository issueTypeRepository;

    @Mock
    private IssueTypeValidator issueTypeValidator;

    @Mock
    private ProjectAuthorizationService projectAuthorizationService;

    @InjectMocks
    private IssueTypeService sut;

    @Nested
    @DisplayName("create issue type")
    class CreateIssueType {

        @Test
        @DisplayName("success: create and save issue type")
        void successCreateIssueType() {
            // given
            Long actorMemberId = 1L;
            ProjectIdentifier pid = ProjectIdentifier.of("WORKSPACE", "PROJ");

            ProjectMember actor = mock(ProjectMember.class);
            Workflow workflow = mock(Workflow.class);
            Project project = mock(Project.class);
            Name typeName = Name.of("Bug");
            IssueType issueType = mock(IssueType.class);

            CreateIssueTypeCommand cmd = CreateIssueTypeCommand.builder()
                    .name(typeName)
                    .description("bug report")
                    .color(ColorType.RED)
                    .icon(IconType.CIRCLE_FILLED)
                    .issueHierarchy(IssueHierarchy.STANDARD)
                    .workflowId(10L)
                    .build();

            given(projectMemberFinder.getWithWorkspaceMember(pid.workspaceKey(), pid.projectKey(), actorMemberId))
                    .willReturn(actor);
            given(workflowFinder.getWithProjectBy(pid.workspaceKey(), pid.projectKey(), cmd.workflowId()))
                    .willReturn(workflow);
            given(workflow.getProject()).willReturn(project);
            given(project.isArchived()).willReturn(false);
            given(issueTypeRepository.save(any(IssueType.class))).willReturn(issueType);

            // when
            sut.create(pid, cmd, actorMemberId);

            // then
            then(projectAuthorizationService).should().requireProjectManager(actor);
            then(issueTypeValidator).should().ensureUniqueLabel(project, typeName);
            then(issueTypeRepository).should().save(any(IssueType.class));
        }

        @Test
        @DisplayName("fail: throws ResourceConflictException if issue type name is duplicate")
        void failCreateIssueType_If_DuplicateName() {
            // given
            Long actorMemberId = 1L;
            ProjectIdentifier pid = ProjectIdentifier.of("WORKSPACE", "PROJ");

            ProjectMember actor = mock(ProjectMember.class);
            Workflow workflow = mock(Workflow.class);
            Project project = mock(Project.class);
            Name typeName = Name.of("Bug");

            CreateIssueTypeCommand cmd = CreateIssueTypeCommand.builder()
                    .name(typeName)
                    .description("bug report")
                    .color(ColorType.RED)
                    .icon(IconType.CIRCLE_FILLED)
                    .issueHierarchy(IssueHierarchy.STANDARD)
                    .workflowId(10L)
                    .build();

            given(projectMemberFinder.getWithWorkspaceMember(pid.workspaceKey(), pid.projectKey(), actorMemberId))
                    .willReturn(actor);
            given(workflowFinder.getWithProjectBy(pid.workspaceKey(), pid.projectKey(), cmd.workflowId()))
                    .willReturn(workflow);
            given(workflow.getProject()).willReturn(project);

            willThrow(new ResourceConflictException(DUPLICATE_ISSUE_TYPE_NAME))
                    .given(issueTypeValidator)
                    .ensureUniqueLabel(project, typeName);

            // when & then
            assertThatThrownBy(() -> sut.create(pid, cmd, actorMemberId)).isInstanceOf(ResourceConflictException.class);
        }
    }

    @Nested
    @DisplayName("rename issue type")
    class RenameIssueType {

        @Test
        @DisplayName("early-return if new and original name is identical when renaming")
        void whenRenaming_EarlyReturn_If_NameUnchanged() {
            // given
            Long actorMemberId = 1L;
            Long issueTypeId = 1L;
            String workspaceKey = "WORKSPACE";

            ProjectMember actor = mock(ProjectMember.class);
            IssueType issueType = mock(IssueType.class);
            Project project = mock(Project.class);

            given(issueTypeFinder.getWithProjectBy(workspaceKey, issueTypeId)).willReturn(issueType);
            given(issueType.getProject()).willReturn(project);
            given(project.getKey()).willReturn("PROJ");
            given(projectMemberFinder.getWithWorkspaceMember(workspaceKey, "PROJ", actorMemberId))
                    .willReturn(actor);
            given(issueType.getName()).willReturn("Bug");

            // when
            sut.update(
                    workspaceKey,
                    issueTypeId,
                    new PatchIssueTypeCommand(
                            JsonNullable.of("Bug"),
                            JsonNullable.undefined(),
                            JsonNullable.undefined(),
                            JsonNullable.undefined()),
                    actorMemberId);

            // then
            then(issueTypeValidator).shouldHaveNoInteractions();
            then(issueType).should(never()).rename(any());
        }
    }

    @Nested
    @DisplayName("delete issue type")
    class DeleteIssueType {

        @Test
        @DisplayName("success: delete issue type")
        void successDeleteIssueType() {
            // given
            Long actorMemberId = 1L;
            Long issueTypeId = 1L;
            String workspaceKey = "WORKSPACE";

            ProjectMember actor = mock(ProjectMember.class);
            IssueType issueType = mock(IssueType.class);
            Project project = mock(Project.class);

            given(issueTypeFinder.getWithProjectBy(workspaceKey, issueTypeId)).willReturn(issueType);
            given(issueType.getProject()).willReturn(project);
            given(project.getKey()).willReturn("PROJ");
            given(projectMemberFinder.getWithWorkspaceMember(workspaceKey, "PROJ", actorMemberId))
                    .willReturn(actor);

            // when
            sut.delete(workspaceKey, issueTypeId, actorMemberId);

            // then
            then(projectAuthorizationService).should().requireProjectManager(actor);
            then(issueTypeValidator).should().ensureDeletable(issueType);
            then(issueTypeRepository).should().delete(issueType);
        }

        @Test
        @DisplayName("fail: throws ForbiddenException if actor's project role is not 'ProjectRole.MANAGER'")
        void failDeleteIssueType_If_NotProjectManager() {
            // given
            Long actorMemberId = 1L;
            Long issueTypeId = 1L;
            String workspaceKey = "WORKSPACE";

            ProjectMember actor = mock(ProjectMember.class);
            IssueType issueType = mock(IssueType.class);
            Project project = mock(Project.class);

            given(issueTypeFinder.getWithProjectBy(workspaceKey, issueTypeId)).willReturn(issueType);
            given(issueType.getProject()).willReturn(project);
            given(project.getKey()).willReturn("PROJ");
            given(projectMemberFinder.getWithWorkspaceMember(workspaceKey, "PROJ", actorMemberId))
                    .willReturn(actor);

            willThrow(new ForbiddenException(PROJECT_MANAGER_REQUIRED))
                    .given(projectAuthorizationService)
                    .requireProjectManager(actor);

            // when & then
            assertThatThrownBy(() -> sut.delete(workspaceKey, issueTypeId, actorMemberId))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("fail: throws BadRequestException if issue type is in use")
        void failDeleteIssueType_If_InUse() {
            // given
            Long actorMemberId = 1L;
            Long issueTypeId = 1L;
            String workspaceKey = "WORKSPACE";

            ProjectMember actor = mock(ProjectMember.class);
            IssueType issueType = mock(IssueType.class);
            Project project = mock(Project.class);

            given(issueTypeFinder.getWithProjectBy(workspaceKey, issueTypeId)).willReturn(issueType);
            given(issueType.getProject()).willReturn(project);
            given(project.getKey()).willReturn("PROJ");
            given(projectMemberFinder.getWithWorkspaceMember(workspaceKey, "PROJ", actorMemberId))
                    .willReturn(actor);

            willThrow(new BadRequestException(ISSUE_TYPE_IN_USE))
                    .given(issueTypeValidator)
                    .ensureDeletable(issueType);

            // when & then
            assertThatThrownBy(() -> sut.delete(workspaceKey, issueTypeId, actorMemberId))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("errorCode")
                    .isEqualTo(ISSUE_TYPE_IN_USE);
        }
    }
}

package com.tissue.feature.issuetype;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.feature.issuetype.application.dto.request.CreateIssueTypeCommand;
import com.tissue.feature.issuetype.application.dto.response.IssueTypeResponse;
import com.tissue.feature.issuetype.application.port.repository.IssueTypeRepository;
import com.tissue.feature.issuetype.application.service.IssueTypeService;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.issuetype.domain.exception.IssueTypeErrorCode;
import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.application.port.repository.ProjectCommandRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberCommandRepository;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.workflow.application.port.repository.WorkflowRepository;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberCommandRepository;
import com.tissue.feature.workspace.application.port.repository.WorkspaceRepository;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.enums.IconType;
import com.tissue.shared.exception.base.ResourceConflictException;
import com.tissue.shared.vo.Name;
import com.tissue.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class IssueTypeServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private IssueTypeService issueTypeService;

    @Autowired
    private IssueTypeRepository issueTypeRepository;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private MemberCommandRepository memberRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberCommandRepository workspaceMemberRepository;

    @Autowired
    private ProjectCommandRepository projectRepository;

    @Autowired
    private ProjectMemberCommandRepository projectMemberRepository;

    private static final ProjectIdentifier PID = new ProjectIdentifier("WORKSPACE", "PROJ");

    private Member member;
    private Long workflowId;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(Member.create("test@tissue.com", "testuser", "HongGilDong"));
        Workspace workspace = workspaceRepository.save(Workspace.create(PID.workspaceKey(), "Test Workspace", null));
        Project project = projectRepository.save(Project.create(workspace, PID.projectKey(), "Test Project", null));
        WorkspaceMember workspaceMember =
                workspaceMemberRepository.save(WorkspaceMember.create(member, workspace, WorkspaceRole.OWNER));
        projectMemberRepository.save(ProjectMember.createManager(project, workspaceMember));

        Workflow workflow = Workflow.create(project, Name.of("Test Workflow"), null, ColorType.GOLD);
        workflow.addState(Name.of("Open"), null, ColorType.GREEN, StateCategory.INITIAL);
        workflow.addState(Name.of("Done"), null, ColorType.BLACK, StateCategory.COMPLETED);
        workflowRepository.save(workflow);
        workflowId = workflow.getId();

        em.flush();
        em.clear();
    }

    @Nested
    @DisplayName("create issue type")
    class CreateIssueType {

        @Test
        @DisplayName("creates issue type")
        void successCreateIssueType() {
            // given
            CreateIssueTypeCommand cmd = CreateIssueTypeCommand.builder()
                    .name(Name.of("Bug"))
                    .description("Bug report")
                    .color(ColorType.RED)
                    .icon(IconType.CIRCLE_FILLED)
                    .issueHierarchy(IssueHierarchy.STANDARD)
                    .workflowId(workflowId)
                    .build();

            // when
            IssueTypeResponse response = issueTypeService.create(PID, cmd, member.getId());
            em.flush();
            em.clear();

            // then
            IssueType issueType = issueTypeRepository
                    .findWithProjectByWorkspaceKeyAndProjectKeyAndId(
                            PID.workspaceKey(), PID.projectKey(), response.issueTypeId())
                    .orElseThrow();

            assertThat(issueType.getName()).isEqualTo("Bug");
            assertThat(issueType.getColor()).isEqualTo(ColorType.RED);
            assertThat(issueType.getIssueHierarchy()).isEqualTo(IssueHierarchy.STANDARD);
            assertThat(issueType.getWorkflow()).isNotNull();
            assertThat(issueType.getWorkflow().getId()).isEqualTo(workflowId);
        }

        @Test
        @DisplayName("fails if issue type name already exists in project")
        void failIfDuplicateName() {
            // given
            CreateIssueTypeCommand cmd = CreateIssueTypeCommand.builder()
                    .name(Name.of("Bug"))
                    .description(null)
                    .color(ColorType.RED)
                    .icon(IconType.CIRCLE_FILLED)
                    .issueHierarchy(IssueHierarchy.STANDARD)
                    .workflowId(workflowId)
                    .build();

            issueTypeService.create(PID, cmd, member.getId());
            em.flush();

            CreateIssueTypeCommand duplicateCmd = CreateIssueTypeCommand.builder()
                    .name(Name.of("Bug"))
                    .description(null)
                    .color(ColorType.BLUE)
                    .icon(IconType.SQUARE_FILLED)
                    .issueHierarchy(IssueHierarchy.STANDARD)
                    .workflowId(workflowId)
                    .build();

            // when & then
            assertThatThrownBy(() -> issueTypeService.create(PID, duplicateCmd, member.getId()))
                    .isInstanceOf(ResourceConflictException.class)
                    .extracting("errorCode")
                    .isEqualTo(IssueTypeErrorCode.DUPLICATE_ISSUE_TYPE_NAME);
        }
    }
}

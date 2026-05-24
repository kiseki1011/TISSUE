package com.tissue.feature.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.application.port.repository.ProjectCommandRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberCommandRepository;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.workflow.application.dto.CreateStateDefinition;
import com.tissue.feature.workflow.application.dto.CreateTransitionDefinition;
import com.tissue.feature.workflow.application.dto.GuardConfigData;
import com.tissue.feature.workflow.application.dto.request.ConfigureTransitionGuardsCommand;
import com.tissue.feature.workflow.application.dto.request.CreateWorkflowCommand;
import com.tissue.feature.workflow.application.dto.response.WorkflowCreateResponse;
import com.tissue.feature.workflow.application.port.repository.WorkflowRepository;
import com.tissue.feature.workflow.application.service.WorkflowCommandService;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.feature.workflow.domain.WorkflowTransition;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import com.tissue.feature.workflow.domain.exception.WorkflowErrorCode;
import com.tissue.feature.workflow.domain.guard.GuardType;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberCommandRepository;
import com.tissue.feature.workspace.application.port.repository.WorkspaceRepository;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.exception.base.BadRequestException;
import com.tissue.shared.vo.Name;
import com.tissue.support.IntegrationTestSupport;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class WorkflowCommandServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private WorkflowCommandService workflowService;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private ProjectCommandRepository projectRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private MemberCommandRepository memberRepository;

    @Autowired
    private WorkspaceMemberCommandRepository workspaceMemberRepository;

    @Autowired
    private ProjectMemberCommandRepository projectMemberRepository;

    private Member member;
    private static final ProjectIdentifier PID = new ProjectIdentifier("WORKSPACE", "PROJ");

    @BeforeEach
    void setUp() {
        member = memberRepository.save(Member.create("test@tissue.com", "testuser", "HongGildong"));
        Workspace workspace = workspaceRepository.save(Workspace.create("WORKSPACE", "Test Workspace", null));
        Project project = projectRepository.save(Project.create(workspace, "PROJ", "Test Project", null));
        WorkspaceMember workspaceMember =
                workspaceMemberRepository.save(WorkspaceMember.create(member, workspace, WorkspaceRole.OWNER));
        projectMemberRepository.save(ProjectMember.create(project, workspaceMember));
        em.flush();
    }

    @Nested
    @DisplayName("create workflow")
    class CreateWorkflow {

        @Test
        @DisplayName("success creating workflow")
        void successCreateWorkflow() {
            // given
            List<CreateStateDefinition> stateDefinitions = List.of(
                    new CreateStateDefinition(
                            "state-1", Name.of("To Do"), null, ColorType.GREEN, StateCategory.INITIAL),
                    new CreateStateDefinition(
                            "state-2", Name.of("In Progress"), null, ColorType.BLUE, StateCategory.ACTIVE),
                    new CreateStateDefinition(
                            "state-3", Name.of("Done"), null, ColorType.BLACK, StateCategory.COMPLETED));

            List<CreateTransitionDefinition> transitionDefinitions = List.of(
                    new CreateTransitionDefinition(Name.of("Start"), null, "state-1", "state-2"),
                    new CreateTransitionDefinition(Name.of("Complete"), null, "state-2", "state-3"));

            CreateWorkflowCommand cmd = CreateWorkflowCommand.builder()
                    .name(Name.of("Test Workflow"))
                    .color(ColorType.YELLOW)
                    .stateDefinitions(stateDefinitions)
                    .transitionDefinitions(transitionDefinitions)
                    .build();

            // when
            WorkflowCreateResponse response = workflowService.create(PID, cmd, member.getId());
            em.flush();
            em.clear();

            // then
            assertThat(response.workspaceKey()).isEqualTo("WORKSPACE");

            Workflow workflow = workflowRepository
                    .findWithProjectByWorkspaceKeyAndProjectKeyAndId("WORKSPACE", "PROJ", response.workflowId())
                    .orElseThrow();

            assertThat(workflow.getName()).isEqualTo("Test Workflow");

            assertThat(workflow.getStates()).hasSize(3);
            assertThat(workflow.getTransitions()).hasSize(2);
            assertThat(workflow.getInitialState()).isNotNull().satisfies(s -> assertThat(s.getDisplayName())
                    .isEqualTo("To Do"));
        }

        @Test
        @DisplayName("fails if workflow graph has no 'COMPLETED' state")
        void failIfNoCompletedState() {
            // given
            CreateWorkflowCommand cmd = CreateWorkflowCommand.builder()
                    .name(Name.of("Invalid Workflow"))
                    .color(ColorType.YELLOW)
                    .stateDefinitions(List.of(
                            new CreateStateDefinition(
                                    "s1", Name.of("Open"), null, ColorType.GREEN, StateCategory.INITIAL),
                            new CreateStateDefinition(
                                    "s2", Name.of("In Progress"), null, ColorType.BLUE, StateCategory.ACTIVE)))
                    .transitionDefinitions(List.of(new CreateTransitionDefinition(Name.of("Start"), null, "s1", "s2")))
                    .build();

            // when & then
            assertThatThrownBy(() -> workflowService.create(PID, cmd, member.getId()))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("errorCode")
                    .isEqualTo(WorkflowErrorCode.MISSING_COMPLETED_STATE);
        }
    }

    @Nested
    @DisplayName("configure transition guard")
    class ConfigureTransitionGuard {

        @Test
        @DisplayName("transition guard is persisted with guard type, params and execution order")
        void guardIsPersisted() {
            // given
            CreateWorkflowCommand createCmd = CreateWorkflowCommand.builder()
                    .name(Name.of("Guard Workflow"))
                    .color(ColorType.YELLOW)
                    .stateDefinitions(List.of(
                            new CreateStateDefinition(
                                    "s1", Name.of("Open"), null, ColorType.GREEN, StateCategory.INITIAL),
                            new CreateStateDefinition(
                                    "s2", Name.of("Done"), null, ColorType.BLACK, StateCategory.COMPLETED)))
                    .transitionDefinitions(
                            List.of(new CreateTransitionDefinition(Name.of("Complete"), null, "s1", "s2")))
                    .build();

            WorkflowCreateResponse created = workflowService.create(PID, createCmd, member.getId());
            em.flush();
            em.clear();

            Workflow workflow = workflowRepository
                    .findWithProjectByWorkspaceKeyAndProjectKeyAndId("WORKSPACE", "PROJ", created.workflowId())
                    .orElseThrow();
            Long transitionId = workflow.getTransitions().getFirst().getId();

            // when
            ConfigureTransitionGuardsCommand guardCmd = new ConfigureTransitionGuardsCommand(List.of(
                    new GuardConfigData(GuardType.ASSIGNEE_REQUIRED, null, 1),
                    new GuardConfigData(
                            GuardType.REQUIRED_APPROVAL,
                            Map.of("min_approvals", 2, "block_on_change_request", true),
                            2)));

            workflowService.configureTransitionGuards(PID, workflow.getId(), transitionId, guardCmd, member.getId());
            em.flush();
            em.clear();

            // then
            Workflow reloaded = workflowRepository
                    .findWithProjectByWorkspaceKeyAndProjectKeyAndId("WORKSPACE", "PROJ", workflow.getId())
                    .orElseThrow();

            WorkflowTransition transition = reloaded.getTransitions().getFirst();
            assertThat(transition.getGuardConfigs()).hasSize(2);
            assertThat(transition.getGuardConfigs().get(0).getGuardType()).isEqualTo(GuardType.ASSIGNEE_REQUIRED);
            assertThat(transition.getGuardConfigs().get(0).getExecutionOrder()).isEqualTo(1);
            assertThat(transition.getGuardConfigs().get(1).getGuardType()).isEqualTo(GuardType.REQUIRED_APPROVAL);
            assertThat(transition.getGuardConfigs().get(1).getGuardParams()).containsEntry("min_approvals", 2);
        }
    }
}

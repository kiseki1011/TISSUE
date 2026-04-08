package com.tissue.feature.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.feature.issuetype.domain.IssueField;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.application.dto.request.CreateProjectCommand;
import com.tissue.feature.project.application.dto.response.ProjectResponse;
import com.tissue.feature.project.application.port.repository.ProjectMemberQueryRepository;
import com.tissue.feature.project.application.port.repository.ProjectQueryRepository;
import com.tissue.feature.project.application.service.ProjectService;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectRole;
import com.tissue.feature.project.domain.exception.DuplicateProjectKeyException;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.feature.workflow.domain.WorkflowTransition;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import com.tissue.feature.workflow.domain.guard.GuardType;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberCommandRepository;
import com.tissue.feature.workspace.application.port.repository.WorkspaceRepository;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.support.IntegrationTestSupport;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ProjectServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private MemberCommandRepository memberCommandRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberCommandRepository workspaceMemberCommandRepository;

    @Autowired
    private ProjectMemberQueryRepository projectMemberQueryRepository;

    @Autowired
    private ProjectQueryRepository projectQueryRepository;

    private Member owner;
    private Workspace workspace;

    @BeforeEach
    void setUp() {
        owner = memberCommandRepository.save(Member.create("owner@tissue.com", "owner", "Jon Snow"));
        workspace = workspaceRepository.save(Workspace.create("WORKSPACE", "Test Workspace", null));
        workspaceMemberCommandRepository.save(WorkspaceMember.create(owner, workspace, WorkspaceRole.OWNER));
        em.flush();
    }

    @Nested
    @DisplayName("create project")
    class CreateProject {

        @Test
        @DisplayName("creating project also creates the creator as MANAGER")
        void creatorBecomesManager() {
            // given
            CreateProjectCommand cmd = new CreateProjectCommand("PROJ", "Test Project", null, null);

            // when
            ProjectResponse response = projectService.create("WORKSPACE", cmd, owner.getId());
            em.flush();
            em.clear();

            // then
            assertThat(response.projectKey()).isEqualTo("PROJ");

            assertThat(projectMemberQueryRepository.findWithWorkspaceMemberByKeysAndMemberId(
                            "WORKSPACE", "PROJ", owner.getId()))
                    .isPresent()
                    .get()
                    .satisfies(pm -> assertThat(pm.getRole()).isEqualTo(ProjectRole.MANAGER));
        }

        @Test
        @DisplayName("creating project sets up default workflows")
        void createsDefaultWorkflows() {
            // given
            CreateProjectCommand cmd = new CreateProjectCommand("PROJ", "Test Project", null, null);

            // when
            projectService.create("WORKSPACE", cmd, owner.getId());
            em.flush();
            em.clear();

            // then
            Project project = projectQueryRepository
                    .findByWorkspaceKeyAndKey("WORKSPACE", "PROJ")
                    .orElseThrow();
            List<Workflow> workflows = project.getWorkflows();

            assertThat(workflows).hasSize(2);
            assertThat(workflows)
                    .allSatisfy(wf -> assertThat(wf.isSystemProvided()).isTrue());

            Workflow reviewWorkflow = workflows.stream()
                    .filter(wf -> wf.getName().equals("Review Workflow"))
                    .findFirst()
                    .orElseThrow();
            assertThat(reviewWorkflow.getStates()).hasSize(5);
            assertThat(reviewWorkflow.getTransitions()).hasSize(6);
            assertThat(reviewWorkflow.getInitialState().getDisplayName()).isEqualTo("To Do");
            assertThat(reviewWorkflow.getStates().stream()
                            .filter(s -> s.isCategorizedAs(StateCategory.COMPLETED))
                            .count())
                    .isEqualTo(1);

            Workflow basicWorkflow = workflows.stream()
                    .filter(wf -> wf.getName().equals("Basic Workflow"))
                    .findFirst()
                    .orElseThrow();
            assertThat(basicWorkflow.getStates()).hasSize(4);
            assertThat(basicWorkflow.getTransitions()).hasSize(3);
        }

        @Test
        @DisplayName("creating project sets up default issue types")
        void createsDefaultIssueTypes() {
            // given
            CreateProjectCommand cmd = new CreateProjectCommand("PROJ", "Test Project", null, null);

            // when
            projectService.create("WORKSPACE", cmd, owner.getId());
            em.flush();
            em.clear();

            // then
            Project project = projectQueryRepository
                    .findByWorkspaceKeyAndKey("WORKSPACE", "PROJ")
                    .orElseThrow();
            List<IssueType> issueTypes = project.getIssueTypes();

            assertThat(issueTypes).hasSize(6);
            assertThat(issueTypes)
                    .allSatisfy(it -> assertThat(it.isSystemProvided()).isTrue());

            assertThat(issueTypes)
                    .extracting(IssueType::getName)
                    .containsExactlyInAnyOrder("Epic", "Story", "Task", "Bug", "Sub Task", "Micro Task");

            assertThat(issueTypes)
                    .extracting(IssueType::getIssueHierarchy)
                    .containsExactlyInAnyOrder(
                            IssueHierarchy.EPIC,
                            IssueHierarchy.STANDARD,
                            IssueHierarchy.STANDARD,
                            IssueHierarchy.STANDARD,
                            IssueHierarchy.SUBTASK,
                            IssueHierarchy.MICROTASK);
        }

        @Test
        @DisplayName("review workflow approve transition has REQUIRED_APPROVAL guard")
        void reviewWorkflowHasApprovalGuard() {
            // given
            CreateProjectCommand cmd = new CreateProjectCommand("PROJ", "Test Project", null, null);

            // when
            projectService.create("WORKSPACE", cmd, owner.getId());
            em.flush();
            em.clear();

            // then
            Project project = projectQueryRepository
                    .findByWorkspaceKeyAndKey("WORKSPACE", "PROJ")
                    .orElseThrow();
            Workflow reviewWorkflow = project.getWorkflows().stream()
                    .filter(wf -> wf.getName().equals("Review Workflow"))
                    .findFirst()
                    .orElseThrow();

            WorkflowTransition approveTransition = reviewWorkflow.getTransitions().stream()
                    .filter(t -> t.getDisplayName().equals("Approve"))
                    .findFirst()
                    .orElseThrow();

            assertThat(approveTransition.getGuardConfigs()).hasSize(1);
            assertThat(approveTransition.getGuardConfigs().getFirst().getGuardType())
                    .isEqualTo(GuardType.REQUIRED_APPROVAL);
        }

        @Test
        @DisplayName("bug issue type has custom fields")
        void bugIssueTypeHasCustomFields() {
            // given
            CreateProjectCommand cmd = new CreateProjectCommand("PROJ", "Test Project", null, null);

            // when
            projectService.create("WORKSPACE", cmd, owner.getId());
            em.flush();
            em.clear();

            // then
            Project project = projectQueryRepository
                    .findByWorkspaceKeyAndKey("WORKSPACE", "PROJ")
                    .orElseThrow();
            IssueType bug = project.getIssueTypes().stream()
                    .filter(it -> it.getName().equals("Bug"))
                    .findFirst()
                    .orElseThrow();

            assertThat(bug.getFields()).hasSize(3);
            assertThat(bug.getFields())
                    .extracting(IssueField::getName)
                    .containsExactly("reproduceSteps", "environment", "version");
        }

        @Test
        @DisplayName("fails if project key already exists in the workspace")
        void failIfProjectKeyDuplicate() {
            // given
            CreateProjectCommand cmd = new CreateProjectCommand("PROJ", "Test Project", null, null);
            projectService.create("WORKSPACE", cmd, owner.getId());
            em.flush();

            CreateProjectCommand duplicateCmd = new CreateProjectCommand("PROJ", "Different Project", null, null);

            // when & then
            assertThatThrownBy(() -> projectService.create("WORKSPACE", duplicateCmd, owner.getId()))
                    .isInstanceOf(DuplicateProjectKeyException.class);
        }
    }
}

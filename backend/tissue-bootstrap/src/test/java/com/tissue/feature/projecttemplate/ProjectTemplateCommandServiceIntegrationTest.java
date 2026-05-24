package com.tissue.feature.projecttemplate;

import static org.assertj.core.api.Assertions.assertThat;

import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.application.dto.request.CreateProjectCommand;
import com.tissue.feature.project.application.port.repository.ProjectCommandRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberCommandRepository;
import com.tissue.feature.project.application.port.repository.ProjectQueryRepository;
import com.tissue.feature.project.application.service.ProjectService;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.projecttemplate.application.dto.request.CreateTemplateFromProjectCommand;
import com.tissue.feature.projecttemplate.application.dto.response.ProjectTemplateResponse;
import com.tissue.feature.projecttemplate.application.port.repository.ProjectTemplateRepository;
import com.tissue.feature.projecttemplate.application.service.ProjectTemplateCommandService;
import com.tissue.feature.projecttemplate.domain.ProjectTemplate;
import com.tissue.feature.projecttemplate.domain.config.TemplateConfig;
import com.tissue.feature.projecttemplate.domain.config.TemplateIssueType;
import com.tissue.feature.projecttemplate.domain.config.TemplateTransition;
import com.tissue.feature.projecttemplate.domain.config.TemplateWorkflow;
import com.tissue.feature.workflow.application.port.repository.WorkflowRepository;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.feature.workflow.domain.WorkflowTransition;
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
class ProjectTemplateCommandServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private ProjectTemplateCommandService projectTemplateCommandService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectTemplateRepository projectTemplateRepository;

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

    @Autowired
    private ProjectQueryRepository projectQueryRepository;

    private Member member;
    private Workspace workspace;
    private WorkspaceMember workspaceMember;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(Member.create("test@tissue.com", "testuser", "Hong GilDong"));
        workspace = workspaceRepository.save(Workspace.create("WORKSPACE", "Test Workspace", null));
        workspaceMember =
                workspaceMemberRepository.save(WorkspaceMember.create(member, workspace, WorkspaceRole.OWNER));
        em.flush();
    }

    @Nested
    @DisplayName("create template from project")
    class CreateFromProject {

        @Test
        @DisplayName("creates template capturing workflows and issue types from project")
        void capturesWorkflowsAndIssueTypes() {
            // given
            projectService.create(
                    "WORKSPACE", new CreateProjectCommand("PROJ", "Test Project", null, null), member.getId());
            em.flush();
            em.clear();

            CreateTemplateFromProjectCommand cmd = CreateTemplateFromProjectCommand.builder()
                    .workspaceKey("WORKSPACE")
                    .projectKey("PROJ")
                    .name("My Template")
                    .description("Template from project")
                    .build();

            // when
            ProjectTemplateResponse response = projectTemplateCommandService.createFromProject(cmd, member.getId());
            em.flush();
            em.clear();

            // then
            ProjectTemplate template =
                    projectTemplateRepository.findById(response.id()).orElseThrow();
            TemplateConfig config = template.getConfigPayload();

            assertThat(template.getName()).isEqualTo("My Template");
            assertThat(template.getDescription()).isEqualTo("Template from project");
            assertThat(config.workflows()).hasSize(2);
            assertThat(config.issueTypes()).hasSize(6);
        }

        @Test
        @DisplayName("template captures transition guards")
        void capturesTransitionGuards() {
            // given
            projectService.create(
                    "WORKSPACE", new CreateProjectCommand("PROJ", "Test Project", null, null), member.getId());
            em.flush();
            em.clear();

            CreateTemplateFromProjectCommand cmd = CreateTemplateFromProjectCommand.builder()
                    .workspaceKey("WORKSPACE")
                    .projectKey("PROJ")
                    .name("Guard Template")
                    .build();

            // when
            ProjectTemplateResponse response = projectTemplateCommandService.createFromProject(cmd, member.getId());
            em.flush();
            em.clear();

            // then
            ProjectTemplate template =
                    projectTemplateRepository.findById(response.id()).orElseThrow();
            TemplateConfig config = template.getConfigPayload();

            TemplateWorkflow reviewWorkflow = config.workflows().stream()
                    .filter(tw -> tw.name().equals("Review Workflow"))
                    .findFirst()
                    .orElseThrow();

            TemplateTransition approveTransition = reviewWorkflow.transitions().stream()
                    .filter(tt -> tt.name().equals("Approve"))
                    .findFirst()
                    .orElseThrow();

            assertThat(approveTransition.guards()).hasSize(1);
            assertThat(approveTransition.guards().getFirst().guardType()).isEqualTo(GuardType.REQUIRED_APPROVAL);
        }

        @Test
        @DisplayName("template captures issue type fields")
        void capturesIssueTypeFields() {
            // given
            projectService.create(
                    "WORKSPACE", new CreateProjectCommand("PROJ", "Test Project", null, null), member.getId());
            em.flush();
            em.clear();

            CreateTemplateFromProjectCommand cmd = CreateTemplateFromProjectCommand.builder()
                    .workspaceKey("WORKSPACE")
                    .projectKey("PROJ")
                    .name("Field Template")
                    .build();

            // when
            ProjectTemplateResponse response = projectTemplateCommandService.createFromProject(cmd, member.getId());
            em.flush();
            em.clear();

            // then
            ProjectTemplate template =
                    projectTemplateRepository.findById(response.id()).orElseThrow();
            TemplateConfig config = template.getConfigPayload();

            TemplateIssueType bugTemplate = config.issueTypes().stream()
                    .filter(tit -> tit.name().equals("Bug"))
                    .findFirst()
                    .orElseThrow();

            assertThat(bugTemplate.fields()).hasSize(3);
            assertThat(bugTemplate.fields())
                    .extracting("name")
                    .containsExactly("reproduceSteps", "environment", "version");
        }
    }

    @Nested
    @DisplayName("apply template to project")
    class ApplyTemplate {

        @Test
        @DisplayName("creating project with template restores workflows, issue types, and guards")
        void restoresFullConfiguration() {
            // given - create source project with default setup
            projectService.create("WORKSPACE", new CreateProjectCommand("SRC", "Source", null, null), member.getId());
            em.flush();
            em.clear();

            // create template from source
            CreateTemplateFromProjectCommand templateCmd = CreateTemplateFromProjectCommand.builder()
                    .workspaceKey("WORKSPACE")
                    .projectKey("SRC")
                    .name("Full Template")
                    .build();
            ProjectTemplateResponse templateResponse =
                    projectTemplateCommandService.createFromProject(templateCmd, member.getId());
            em.flush();
            em.clear();

            // when - create new project using the template
            CreateProjectCommand projectCmd =
                    new CreateProjectCommand("DST", "Destination", null, templateResponse.id());
            projectService.create("WORKSPACE", projectCmd, member.getId());
            em.flush();
            em.clear();

            // then
            Project dstProject = projectQueryRepository
                    .findByWorkspaceKeyAndKey("WORKSPACE", "DST")
                    .orElseThrow();

            List<Workflow> workflows = dstProject.getWorkflows();
            assertThat(workflows).hasSize(2);

            Workflow reviewWorkflow = workflows.stream()
                    .filter(wf -> wf.getName().equals("Review Workflow"))
                    .findFirst()
                    .orElseThrow();
            assertThat(reviewWorkflow.getStates()).hasSize(5);
            assertThat(reviewWorkflow.getTransitions()).hasSize(6);

            WorkflowTransition approveTransition = reviewWorkflow.getTransitions().stream()
                    .filter(t -> t.getDisplayName().equals("Approve"))
                    .findFirst()
                    .orElseThrow();
            assertThat(approveTransition.getGuardConfigs()).hasSize(1);
            assertThat(approveTransition.getGuardConfigs().getFirst().getGuardType())
                    .isEqualTo(GuardType.REQUIRED_APPROVAL);

            List<IssueType> issueTypes = dstProject.getIssueTypes();
            assertThat(issueTypes).hasSize(6);
            assertThat(issueTypes)
                    .extracting(IssueType::getName)
                    .containsExactlyInAnyOrder("Epic", "Story", "Task", "Bug", "Sub Task", "Micro Task");

            // template-created resources should NOT be system provided
            assertThat(workflows)
                    .allSatisfy(wf -> assertThat(wf.isSystemProvided()).isFalse());
            assertThat(issueTypes)
                    .allSatisfy(it -> assertThat(it.isSystemProvided()).isFalse());
        }
    }
}

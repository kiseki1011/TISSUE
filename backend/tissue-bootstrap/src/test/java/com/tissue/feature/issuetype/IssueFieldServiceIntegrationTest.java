package com.tissue.feature.issuetype;

import static org.assertj.core.api.Assertions.assertThat;

import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.feature.issuetype.application.dto.request.CreateIssueFieldCommand;
import com.tissue.feature.issuetype.application.dto.response.IssueFieldResponse;
import com.tissue.feature.issuetype.application.port.repository.IssueFieldRepository;
import com.tissue.feature.issuetype.application.port.repository.IssueTypeRepository;
import com.tissue.feature.issuetype.application.service.IssueFieldService;
import com.tissue.feature.issuetype.domain.FieldOption;
import com.tissue.feature.issuetype.domain.IssueField;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.issuetype.domain.enums.IssueFieldType;
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
import com.tissue.shared.vo.Name;
import com.tissue.support.IntegrationTestSupport;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class IssueFieldServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private IssueFieldService issueFieldService;

    @Autowired
    private IssueTypeRepository issueTypeRepository;

    @Autowired
    private IssueFieldRepository issueFieldRepository;

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
    private Long issueTypeId;

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

        IssueType issueType = IssueType.create(
                project,
                Name.of("Bug"),
                null,
                ColorType.RED,
                IconType.CIRCLE_FILLED,
                IssueHierarchy.STANDARD,
                workflow);
        issueTypeRepository.save(issueType);
        issueTypeId = issueType.getId();

        em.flush();
        em.clear();
    }

    @Nested
    @DisplayName("add field")
    class AddField {

        @Test
        @DisplayName("adds 'SELECT_OPTION' field with initial options")
        void addSelectOptionFieldWithOptions() {
            // given
            CreateIssueFieldCommand cmd = CreateIssueFieldCommand.builder()
                    .name(Name.of("Priority"))
                    .description("Issue priority")
                    .issueFieldType(IssueFieldType.SELECT_OPTION)
                    .required(true)
                    .initialOptions(List.of(Name.of("Low"), Name.of("Medium"), Name.of("High")))
                    .position(0)
                    .build();

            // when
            IssueFieldResponse response =
                    issueFieldService.addField(PID.workspaceKey(), issueTypeId, cmd, member.getId());
            em.flush();
            em.clear();

            // then
            IssueField field = issueFieldRepository
                    .findWithProjectAndIssueTypeByWorkspaceKeyAndId(PID.workspaceKey(), response.issueFieldId())
                    .orElseThrow();

            assertThat(field.getName()).isEqualTo("Priority");
            assertThat(field.getIssueFieldType()).isEqualTo(IssueFieldType.SELECT_OPTION);
            assertThat(field.isRequired()).isTrue();
            assertThat(field.getOptions()).hasSize(3);
            assertThat(field.getOptions())
                    .extracting(FieldOption::getName)
                    .containsExactlyInAnyOrder("Low", "Medium", "High");
        }

        @Test
        @DisplayName("multiple fields must be ordered by position")
        void fieldsOrderedByPosition() {
            // given
            issueFieldService.addField(
                    PID.workspaceKey(),
                    issueTypeId,
                    CreateIssueFieldCommand.builder()
                            .name(Name.of("Description"))
                            .issueFieldType(IssueFieldType.TEXT)
                            .required(false)
                            .initialOptions(List.of())
                            .position(2)
                            .build(),
                    member.getId());

            issueFieldService.addField(
                    PID.workspaceKey(),
                    issueTypeId,
                    CreateIssueFieldCommand.builder()
                            .name(Name.of("Priority"))
                            .issueFieldType(IssueFieldType.SELECT_OPTION)
                            .required(true)
                            .initialOptions(List.of(Name.of("Low"), Name.of("High")))
                            .position(0)
                            .build(),
                    member.getId());

            issueFieldService.addField(
                    PID.workspaceKey(),
                    issueTypeId,
                    CreateIssueFieldCommand.builder()
                            .name(Name.of("Due Date"))
                            .issueFieldType(IssueFieldType.DATE)
                            .required(false)
                            .initialOptions(List.of())
                            .position(1)
                            .build(),
                    member.getId());

            em.flush();
            em.clear();

            // when
            IssueType reloaded = issueTypeRepository
                    .findWithProjectByWorkspaceKeyAndProjectKeyAndId(PID.workspaceKey(), PID.projectKey(), issueTypeId)
                    .orElseThrow();

            // then
            assertThat(reloaded.getFields()).hasSize(3);
            assertThat(reloaded.getFields())
                    .extracting(IssueField::getName)
                    .containsExactly("Priority", "Due Date", "Description");
        }
    }
}

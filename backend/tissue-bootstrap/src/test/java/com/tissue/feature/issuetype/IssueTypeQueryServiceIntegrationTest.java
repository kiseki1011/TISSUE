package com.tissue.feature.issuetype;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.feature.issuetype.application.dto.response.IssueTypeDetail;
import com.tissue.feature.issuetype.application.dto.response.IssueTypeSummary;
import com.tissue.feature.issuetype.application.port.repository.IssueTypeRepository;
import com.tissue.feature.issuetype.application.service.IssueTypeQueryService;
import com.tissue.feature.issuetype.domain.IssueField;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.issuetype.domain.enums.IssueFieldType;
import com.tissue.feature.issuetype.domain.exception.IssueTypeNotFoundException;
import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.application.port.repository.ProjectCommandRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberCommandRepository;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.project.domain.exception.ProjectMemberNotFoundException;
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
class IssueTypeQueryServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private IssueTypeQueryService sut;

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

    private static final ProjectIdentifier PROJECT_IDENTIFIER = ProjectIdentifier.of("WORKSPACE", "PROJ");

    private Member gildong;
    private Member bob;
    private Project project;
    private Workflow workflow;

    @BeforeEach
    void setUp() {
        gildong = memberRepository.save(Member.create("gildong@tissue.com", "gildong", "Hong Gildong"));
        bob = memberRepository.save(Member.create("bob@tissue.com", "bob", "Bob"));

        Workspace workspace =
                workspaceRepository.save(Workspace.create(PROJECT_IDENTIFIER.workspaceKey(), "Workspace", null));
        project = projectRepository.save(Project.create(workspace, PROJECT_IDENTIFIER.projectKey(), "Project", null));

        WorkspaceMember gildongWorkspaceMember =
                workspaceMemberRepository.save(WorkspaceMember.create(gildong, workspace, WorkspaceRole.OWNER));
        projectMemberRepository.save(ProjectMember.createManager(project, gildongWorkspaceMember));

        workflow = Workflow.create(project, Name.of("Default Workflow"), null, ColorType.YELLOW);
        workflow.addState(Name.of("Open"), null, ColorType.GREEN, StateCategory.INITIAL);
        workflow.addState(Name.of("Done"), null, ColorType.BLACK, StateCategory.COMPLETED);
        workflowRepository.save(workflow);

        em.flush();
        em.clear();
    }

    private IssueType saveIssueType(String name) {
        Project managedProject = em.find(Project.class, project.getId());
        Workflow managedWorkflow = em.find(Workflow.class, workflow.getId());
        IssueType issueType = IssueType.create(
                managedProject,
                Name.of(name),
                "desc",
                ColorType.RED,
                IconType.CIRCLE_FILLED,
                IssueHierarchy.STANDARD,
                managedWorkflow);
        return issueTypeRepository.save(issueType);
    }

    @Nested
    @DisplayName("getProjectIssueTypes")
    class GetProjectIssueTypes {

        @Test
        @DisplayName("returns every issue type of the project")
        void returnsAllIssueTypes() {
            // given
            saveIssueType("Bug");
            saveIssueType("Story");
            em.flush();
            em.clear();

            // when
            List<IssueTypeSummary> result = sut.getProjectIssueTypes(PROJECT_IDENTIFIER, gildong.getId());

            // then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(IssueTypeSummary::name).containsExactlyInAnyOrder("Bug", "Story");
            assertThat(result).allMatch(summary -> summary.workflowId().equals(workflow.getId()));
            assertThat(result).allMatch(summary -> "Default Workflow".equals(summary.workflowName()));
        }

        @Test
        @DisplayName("returns an empty list when the project has no issue types")
        void returnsEmptyListWhenNoIssueTypes() {
            // when
            List<IssueTypeSummary> result = sut.getProjectIssueTypes(PROJECT_IDENTIFIER, gildong.getId());

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("rejects a non project member")
        void rejectsNonProjectMember() {
            // when & then
            assertThatThrownBy(() -> sut.getProjectIssueTypes(PROJECT_IDENTIFIER, bob.getId()))
                    .isInstanceOf(ProjectMemberNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getIssueTypeDetail")
    class GetIssueTypeDetail {

        @Test
        @DisplayName("returns the issue type with its ordered fields and options")
        void returnsDetailWithFieldsAndOptions() {
            // given
            IssueType issueType = saveIssueType("Bug");
            IssueField statusField =
                    issueType.addField(Name.of("Severity"), "severity level", IssueFieldType.SELECT_OPTION, true, 0);
            statusField.addOption(Name.of("Low"));
            statusField.addOption(Name.of("High"));
            issueType.addField(Name.of("Note"), "free text", IssueFieldType.TEXT, false, 1);
            em.flush();
            em.clear();

            // when
            IssueTypeDetail detail = sut.getIssueTypeDetail(PROJECT_IDENTIFIER, issueType.getId(), gildong.getId());

            // then
            assertThat(detail.id()).isEqualTo(issueType.getId());
            assertThat(detail.name()).isEqualTo("Bug");
            assertThat(detail.workflowId()).isEqualTo(workflow.getId());
            assertThat(detail.workflowName()).isEqualTo("Default Workflow");
            assertThat(detail.fields()).hasSize(2);
            assertThat(detail.fields().getFirst().name()).isEqualTo("Severity");
            assertThat(detail.fields().getFirst().type()).isEqualTo(IssueFieldType.SELECT_OPTION);
            assertThat(detail.fields().getFirst().options()).extracting("name").containsExactly("Low", "High");
            assertThat(detail.fields().get(1).name()).isEqualTo("Note");
            assertThat(detail.fields().get(1).options()).isEmpty();
        }

        @Test
        @DisplayName("returns the issue type with an empty fields list when no fields exist")
        void returnsDetailWithEmptyFields() {
            // given
            IssueType issueType = saveIssueType("Bug");
            em.flush();
            em.clear();

            // when
            IssueTypeDetail detail = sut.getIssueTypeDetail(PROJECT_IDENTIFIER, issueType.getId(), gildong.getId());

            // then
            assertThat(detail.fields()).isEmpty();
        }

        @Test
        @DisplayName("rejects non project member")
        void rejectsNonProjectMember() {
            // given
            IssueType issueType = saveIssueType("Bug");
            em.flush();
            em.clear();

            // when & then
            assertThatThrownBy(() -> sut.getIssueTypeDetail(PROJECT_IDENTIFIER, issueType.getId(), bob.getId()))
                    .isInstanceOf(ProjectMemberNotFoundException.class);
        }

        @Test
        @DisplayName("throws when the issue type does not exist")
        void throwsWhenNotFound() {
            // when & then
            assertThatThrownBy(() -> sut.getIssueTypeDetail(PROJECT_IDENTIFIER, 999L, gildong.getId()))
                    .isInstanceOf(IssueTypeNotFoundException.class);
        }
    }
}

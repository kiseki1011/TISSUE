package com.tissue.feature.sprint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.issue.application.dto.request.CreateIssueCommand;
import com.tissue.feature.issue.application.port.repository.IssueQueryRepository;
import com.tissue.feature.issue.application.service.IssueLifecycleService;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.feature.issue.domain.enums.IssuePriority;
import com.tissue.feature.issuetype.application.port.repository.IssueTypeRepository;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.application.port.repository.ProjectCommandRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberCommandRepository;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.sprint.application.dto.request.MigrateSprintIssuesCommand;
import com.tissue.feature.sprint.application.port.repository.SprintCommandRepository;
import com.tissue.feature.sprint.application.service.SprintCommandService;
import com.tissue.feature.sprint.domain.Sprint;
import com.tissue.feature.sprint.domain.exception.SprintErrorCode;
import com.tissue.feature.sprint.domain.exception.SprintNotFoundException;
import com.tissue.feature.workflow.application.port.repository.WorkflowRepository;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.feature.workflow.domain.WorkflowState;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.enums.IconType;
import com.tissue.shared.exception.TissueException;
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
public class SprintCommandServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private SprintCommandService sprintCommandService;

    @Autowired
    private IssueLifecycleService issueLifecycleService;

    @Autowired
    private IssueQueryRepository issueQueryRepository;

    @Autowired
    private SprintCommandRepository sprintRepository;

    @Autowired
    private IssueTypeRepository issueTypeRepository;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private MemberCommandRepository memberRepository;

    @Autowired
    private ProjectCommandRepository projectRepository;

    @Autowired
    private ProjectMemberCommandRepository projectMemberRepository;

    private static final ProjectIdentifier PID = ProjectIdentifier.ofProjectKey("PROJ");
    private static final ProjectIdentifier OUTSIDER_PID = ProjectIdentifier.ofProjectKey("MINE");

    private Member member;
    private Member outsider;
    private Project project;
    private Project outsiderProject;
    private Long workflowId;
    private Long issueTypeId;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(Member.create("test@tissue.com", "testuser", "HongGilDong"));
        project = projectRepository.save(Project.create("PROJ", "Test Project", null));
        projectMemberRepository.save(ProjectMember.createManager(project, member));

        outsider = memberRepository.save(Member.create("outsider@tissue.com", "outsider", "Outsider"));
        outsiderProject = projectRepository.save(Project.create("MINE", "Outsider Project", null));
        projectMemberRepository.save(ProjectMember.createManager(outsiderProject, outsider));

        Workflow workflow = Workflow.create(Name.of("Test Workflow"), null, ColorType.ANSI_YELLOW);
        WorkflowState todo = workflow.addState(Name.of("TODO"), null, ColorType.ANSI_GREEN, StateCategory.INITIAL);
        WorkflowState done = workflow.addState(Name.of("DONE"), null, ColorType.ANSI_BLACK, StateCategory.COMPLETED);
        workflow.addTransition(Name.of("Complete"), null, todo, done);
        workflowRepository.save(workflow);
        workflowId = workflow.getId();

        IssueType issueType = IssueType.create(
                Name.of("Story"), null, ColorType.ANSI_RED, IconType.CIRCLE_FILLED, IssueHierarchy.STANDARD, workflow);
        issueTypeRepository.save(issueType);
        issueTypeId = issueType.getId();

        em.flush();
        em.clear();
    }

    @Nested
    @DisplayName("migrate issues")
    class MigrateIssues {

        @Test
        @DisplayName("moves only incomplete issues to the target sprint")
        void movesOnlyIncompleteIssuesToTheTargetSprint() {
            // given
            Long sourceSprintId = createSprint(project, "Source");
            Long targetSprintId = createSprint(project, "Target");
            String incompleteKey1 = createIssue(PID, sourceSprintId, member.getId());
            String incompleteKey2 = createIssue(PID, sourceSprintId, member.getId());
            String completedKey = createIssue(PID, sourceSprintId, member.getId());
            transitionToDone(completedKey);

            // when
            sprintCommandService.migrateIssues(
                    sourceSprintId, new MigrateSprintIssuesCommand(targetSprintId), member.getId());
            em.flush();
            em.clear();

            // then
            for (String key : List.of(incompleteKey1, incompleteKey2)) {
                assertThat(issueQueryRepository
                                .findByKey(key)
                                .orElseThrow()
                                .getSprint()
                                .getId())
                        .isEqualTo(targetSprintId);
            }
            assertThat(issueQueryRepository
                            .findByKey(completedKey)
                            .orElseThrow()
                            .getSprint()
                            .getId())
                    .isEqualTo(sourceSprintId);
        }

        @Test
        @DisplayName("a target sprint of another project resolves like a nonexistent one")
        void rejectsTargetSprintOfAnotherProject() {
            // given
            Long sourceSprintId = createSprint(project, "Source");
            Long foreignSprintId = createSprint(outsiderProject, "Foreign");
            String issueKey = createIssue(PID, sourceSprintId, member.getId());

            // when & then — manager of PROJ points the migration at MINE's sprint
            assertThatThrownBy(() -> sprintCommandService.migrateIssues(
                            sourceSprintId, new MigrateSprintIssuesCommand(foreignSprintId), member.getId()))
                    .isInstanceOf(SprintNotFoundException.class);

            em.flush();
            em.clear();
            assertThat(issueQueryRepository
                            .findByKey(issueKey)
                            .orElseThrow()
                            .getSprint()
                            .getId())
                    .isEqualTo(sourceSprintId);
        }
    }

    @Nested
    @DisplayName("add issues")
    class AddIssues {

        @Test
        @DisplayName("rejects an issue of another project")
        void rejectsIssueOfAnotherProject() {
            // given
            Long sprintId = createSprint(project, "Sprint");
            String foreignIssueKey = createIssue(OUTSIDER_PID, null, outsider.getId());

            // when & then
            assertThatThrownBy(() -> sprintCommandService.addIssues(sprintId, List.of(foreignIssueKey), member.getId()))
                    .isInstanceOf(BadRequestException.class)
                    .extracting(e -> ((TissueException) e).getErrorCode())
                    .isEqualTo(SprintErrorCode.SPRINT_ISSUE_PROJECT_MISMATCH);

            em.flush();
            em.clear();
            assertThat(issueQueryRepository
                            .findByKey(foreignIssueKey)
                            .orElseThrow()
                            .getSprint())
                    .isNull();
        }
    }

    private Long createSprint(Project owner, String title) {
        Sprint sprint = sprintRepository.save(Sprint.create(owner, title, null));
        em.flush();
        em.clear();
        return sprint.getId();
    }

    private String createIssue(ProjectIdentifier pid, Long sprintId, Long actorMemberId) {
        CreateIssueCommand cmd = CreateIssueCommand.builder()
                .title("Test Issue")
                .priority(IssuePriority.P2)
                .issueTypeId(issueTypeId)
                .sprintId(sprintId)
                .customFields(Map.of())
                .build();

        String issueKey = issueLifecycleService.create(pid, cmd, actorMemberId).issueKey();
        em.flush();
        em.clear();
        return issueKey;
    }

    private void transitionToDone(String issueKey) {
        Issue issue = issueQueryRepository.findByKey(issueKey).orElseThrow();
        Workflow workflow = workflowRepository.findById(workflowId).orElseThrow();
        WorkflowState done = workflow.getStates().stream()
                .filter(s -> s.getCategory() == StateCategory.COMPLETED)
                .findFirst()
                .orElseThrow();
        issue.transitionTo(done);
        em.flush();
        em.clear();
    }
}

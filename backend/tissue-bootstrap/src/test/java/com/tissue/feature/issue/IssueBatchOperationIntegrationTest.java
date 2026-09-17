package com.tissue.feature.issue;

import static org.assertj.core.api.Assertions.assertThat;

import com.tissue.feature.issue.application.dto.request.BatchChangeParentCommand;
import com.tissue.feature.issue.application.dto.request.BatchDeleteCommand;
import com.tissue.feature.issue.application.dto.request.BatchRemoveParentCommand;
import com.tissue.feature.issue.application.dto.request.CreateIssueCommand;
import com.tissue.feature.issue.application.port.repository.IssueQueryRepository;
import com.tissue.feature.issue.application.service.IssueLifecycleService;
import com.tissue.feature.issue.application.service.IssueUpdateService;
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
import com.tissue.feature.workflow.application.port.repository.WorkflowRepository;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.feature.workflow.domain.WorkflowState;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import com.tissue.shared.dto.BatchOperationResponse;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.enums.IconType;
import com.tissue.shared.vo.Name;
import com.tissue.support.IntegrationTestSupport;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class IssueBatchOperationIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private IssueLifecycleService issueLifecycleService;

    @Autowired
    private IssueUpdateService issueUpdateService;

    @Autowired
    private IssueQueryRepository issueQueryRepository;

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
    private Long standardTypeId;
    private Long epicTypeId;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(Member.create("test@tissue.com", "testuser", "HongGilDong"));
        Project project = projectRepository.save(Project.create("PROJ", "Test Project", null));
        projectMemberRepository.save(ProjectMember.createManager(project, member));

        outsider = memberRepository.save(Member.create("outsider@tissue.com", "outsider", "Outsider"));
        Project outsiderProject = projectRepository.save(Project.create("MINE", "Outsider Project", null));
        projectMemberRepository.save(ProjectMember.createManager(outsiderProject, outsider));

        Workflow workflow = Workflow.create(Name.of("Test Workflow"), null, ColorType.ANSI_YELLOW);
        WorkflowState todo = workflow.addState(Name.of("TODO"), null, ColorType.ANSI_GREEN, StateCategory.INITIAL);
        WorkflowState done = workflow.addState(Name.of("DONE"), null, ColorType.ANSI_BLACK, StateCategory.COMPLETED);
        workflow.addTransition(Name.of("Complete"), null, todo, done);
        workflowRepository.save(workflow);

        IssueType standardType = IssueType.create(
                Name.of("Story"), null, ColorType.ANSI_RED, IconType.CIRCLE_FILLED, IssueHierarchy.STANDARD, workflow);
        issueTypeRepository.save(standardType);
        standardTypeId = standardType.getId();

        IssueType epicType = IssueType.create(
                Name.of("Epic"), null, ColorType.ANSI_BLUE, IconType.CIRCLE_FILLED, IssueHierarchy.EPIC, workflow);
        issueTypeRepository.save(epicType);
        epicTypeId = epicType.getId();

        em.flush();
        em.clear();
    }

    @Nested
    @DisplayName("batch delete")
    class BatchDelete {

        @Test
        @DisplayName("soft-deletes every requested issue in the project")
        void successBatchDelete_InOwnProject() {
            // given
            String key1 = createIssue(PID, standardTypeId, null, member.getId());
            String key2 = createIssue(PID, standardTypeId, null, member.getId());

            // when
            BatchOperationResponse result =
                    issueLifecycleService.batchDelete(PID, new BatchDeleteCommand(Set.of(key1, key2)), member.getId());
            em.flush();
            em.clear();

            // then
            assertThat(result.totalCount()).isEqualTo(2);
            assertThat(result.failures()).isEmpty();
            assertThat(issueQueryRepository.findByKey(key1)).isEmpty();
            assertThat(issueQueryRepository.findDeletedWithProjectByKey(key2)).isPresent();
        }

        @Test
        @DisplayName("issue keys of another project resolve like nonexistent ones and survive")
        void batchDeleteIgnoresIssuesOfAnotherProject() {
            // given
            String victimKey = createIssue(PID, standardTypeId, null, member.getId());

            // when — outsider targets PROJ's issue through their own project's endpoint
            BatchOperationResponse result = issueLifecycleService.batchDelete(
                    OUTSIDER_PID, new BatchDeleteCommand(Set.of(victimKey)), outsider.getId());
            em.flush();
            em.clear();

            // then
            assertThat(result.totalCount()).isZero();
            assertThat(issueQueryRepository.findByKey(victimKey)).isPresent();
        }
    }

    @Nested
    @DisplayName("batch assign parent")
    class BatchAssignParent {

        @Test
        @DisplayName("assigns an epic parent to issues in the project")
        void successBatchAssignParent_InOwnProject() {
            // given
            String epicKey = createIssue(PID, epicTypeId, null, member.getId());
            String childKey = createIssue(PID, standardTypeId, null, member.getId());

            // when
            BatchOperationResponse result = issueUpdateService.batchAssignParent(
                    PID, new BatchChangeParentCommand(Set.of(childKey), epicKey), member.getId());
            em.flush();
            em.clear();

            // then
            assertThat(result.totalCount()).isEqualTo(1);
            assertThat(result.failures()).isEmpty();

            Issue child = issueQueryRepository.findByKey(childKey).orElseThrow();
            assertThat(child.getParentIssue().getKey()).isEqualTo(epicKey);
        }

        @Test
        @DisplayName("issue keys of another project resolve like nonexistent ones and keep their parent state")
        void batchAssignParentIgnoresIssuesOfAnotherProject() {
            // given
            String victimKey = createIssue(PID, standardTypeId, null, member.getId());
            String outsiderEpicKey = createIssue(OUTSIDER_PID, epicTypeId, null, outsider.getId());

            // when
            BatchOperationResponse result = issueUpdateService.batchAssignParent(
                    OUTSIDER_PID, new BatchChangeParentCommand(Set.of(victimKey), outsiderEpicKey), outsider.getId());
            em.flush();
            em.clear();

            // then
            assertThat(result.totalCount()).isZero();

            Issue victim = issueQueryRepository.findByKey(victimKey).orElseThrow();
            assertThat(victim.getParentIssue()).isNull();
        }
    }

    @Nested
    @DisplayName("batch remove parent")
    class BatchRemoveParent {

        @Test
        @DisplayName("removes the parent from issues in the project")
        void successBatchRemoveParent_InOwnProject() {
            // given
            String epicKey = createIssue(PID, epicTypeId, null, member.getId());
            String childKey = createIssue(PID, standardTypeId, epicKey, member.getId());

            // when
            BatchOperationResponse result = issueUpdateService.batchRemoveParent(
                    PID, new BatchRemoveParentCommand(Set.of(childKey)), member.getId());
            em.flush();
            em.clear();

            // then
            assertThat(result.totalCount()).isEqualTo(1);
            assertThat(result.failures()).isEmpty();
            assertThat(issueQueryRepository.findByKey(childKey).orElseThrow().getParentIssue())
                    .isNull();
        }

        @Test
        @DisplayName("issue keys of another project resolve like nonexistent ones and keep their parent")
        void batchRemoveParentIgnoresIssuesOfAnotherProject() {
            // given
            String epicKey = createIssue(PID, epicTypeId, null, member.getId());
            String victimKey = createIssue(PID, standardTypeId, epicKey, member.getId());

            // when
            BatchOperationResponse result = issueUpdateService.batchRemoveParent(
                    OUTSIDER_PID, new BatchRemoveParentCommand(Set.of(victimKey)), outsider.getId());
            em.flush();
            em.clear();

            // then
            assertThat(result.totalCount()).isZero();
            assertThat(issueQueryRepository.findByKey(victimKey).orElseThrow().getParentIssue())
                    .isNotNull();
        }
    }

    private String createIssue(ProjectIdentifier pid, Long issueTypeId, String parentKey, Long actorMemberId) {
        CreateIssueCommand cmd = CreateIssueCommand.builder()
                .title("Test Issue")
                .priority(IssuePriority.P2)
                .issueTypeId(issueTypeId)
                .parentKey(parentKey)
                .customFields(Map.of())
                .build();

        String issueKey = issueLifecycleService.create(pid, cmd, actorMemberId).issueKey();
        em.flush();
        em.clear();
        return issueKey;
    }
}

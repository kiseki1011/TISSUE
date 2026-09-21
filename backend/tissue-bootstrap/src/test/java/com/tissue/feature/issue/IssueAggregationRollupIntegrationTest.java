package com.tissue.feature.issue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.tissue.feature.issue.application.dto.request.CreateIssueCommand;
import com.tissue.feature.issue.application.port.repository.IssueQueryRepository;
import com.tissue.feature.issue.application.service.IssueLifecycleService;
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
import com.tissue.shared.dto.IssueIdentifier;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.enums.IconType;
import com.tissue.shared.vo.Name;
import com.tissue.support.IntegrationTestSupport;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

class IssueAggregationRollupIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private IssueLifecycleService issueLifecycleService;

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

    @Autowired
    private TransactionTemplate transactionTemplate;

    private static final ProjectIdentifier PID = ProjectIdentifier.ofProjectKey("PROJ");

    private Member member;
    private Long epicTypeId;
    private Long standardTypeId;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(Member.create("test@tissue.com", "testuser", "HongGilDong"));
        Project project = projectRepository.save(Project.create("PROJ", "Test Project", null));
        projectMemberRepository.save(ProjectMember.createManager(project, member));

        Workflow workflow = Workflow.create(Name.of("Test Workflow"), null, ColorType.ANSI_YELLOW);
        WorkflowState todo = workflow.addState(Name.of("TODO"), null, ColorType.ANSI_GREEN, StateCategory.INITIAL);
        WorkflowState done = workflow.addState(Name.of("DONE"), null, ColorType.ANSI_BLACK, StateCategory.COMPLETED);
        workflow.addTransition(Name.of("Complete"), null, todo, done);
        workflowRepository.save(workflow);

        IssueType epicType = IssueType.create(
                Name.of("Epic"), null, ColorType.ANSI_BLUE, IconType.CIRCLE_FILLED, IssueHierarchy.EPIC, workflow);
        issueTypeRepository.save(epicType);
        epicTypeId = epicType.getId();

        IssueType standardType = IssueType.create(
                Name.of("Story"), null, ColorType.ANSI_RED, IconType.CIRCLE_FILLED, IssueHierarchy.STANDARD, workflow);
        issueTypeRepository.save(standardType);
        standardTypeId = standardType.getId();
    }

    @Test
    @DisplayName("an epic's story point rolls up its children's, and back down when a child is deleted")
    void epicStoryPointRollsUpChildrenAndBackDownOnDelete() {
        // given - an epic with no children yet
        String epicKey = transactionTemplate.execute(status -> createIssue(epicTypeId, null, null));

        // when - a 3-point child is added
        transactionTemplate.execute(status -> createIssue(standardTypeId, epicKey, 3));

        // then - the rollup reaches 3
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(epicStoryPoint(epicKey)).isEqualTo(3));

        // when - a 5-point child is added
        String childToDelete = transactionTemplate.execute(status -> createIssue(standardTypeId, epicKey, 5));

        // then - the rollup climbs to 8
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(epicStoryPoint(epicKey)).isEqualTo(8));

        // when - the 5-point child is deleted
        transactionTemplate.executeWithoutResult(
                status -> issueLifecycleService.delete(IssueIdentifier.ofIssueKey(childToDelete), member.getId()));

        // then - the rollup drops back to the remaining child's 3
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(epicStoryPoint(epicKey)).isEqualTo(3));
    }

    private Integer epicStoryPoint(String epicKey) {
        return transactionTemplate.execute(
                status -> issueQueryRepository.findByKey(epicKey).orElseThrow().getStoryPoint());
    }

    private String createIssue(Long issueTypeId, String parentKey, Integer storyPoint) {
        CreateIssueCommand cmd = CreateIssueCommand.builder()
                .title("Test Issue")
                .priority(IssuePriority.P2)
                .issueTypeId(issueTypeId)
                .parentKey(parentKey)
                .storyPoint(storyPoint)
                .customFields(Map.of())
                .build();

        return issueLifecycleService.create(PID, cmd, member.getId()).issueKey();
    }
}

package com.tissue.feature.issue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.issue.application.dto.request.CreateIssueCommand;
import com.tissue.feature.issue.application.dto.response.IssueCreateResponse;
import com.tissue.feature.issue.application.port.repository.IssueQueryRepository;
import com.tissue.feature.issue.application.service.IssueLifecycleService;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.feature.issue.domain.enums.IssuePriority;
import com.tissue.feature.issue.domain.exception.IssueErrorCode;
import com.tissue.feature.issuetype.application.port.repository.IssueTypeRepository;
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
import com.tissue.feature.workflow.domain.WorkflowState;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import com.tissue.shared.dto.IssueIdentifier;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.enums.IconType;
import com.tissue.shared.exception.base.BadRequestException;
import com.tissue.shared.vo.Name;
import com.tissue.support.IntegrationTestSupport;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class IssueLifecycleServiceIntegrationTest extends IntegrationTestSupport {

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

    private static final ProjectIdentifier PID = ProjectIdentifier.ofProjectKey("PROJ");

    private Member member;
    private Long workflowId;
    private Long issueTypeId;
    private Long fieldId1;
    private Long fieldId2;

    @BeforeEach
    void setUp() {
        // setup Member & Project
        member = memberRepository.save(Member.create("test@tissue.com", "testuser", "HongGilDong"));

        Project project = projectRepository.save(Project.create("PROJ", "Test Project", null));
        projectMemberRepository.save(ProjectMember.createManager(project, member));

        // setup global Workflow
        Workflow workflow = Workflow.create(Name.of("Test Workflow"), null, ColorType.ANSI_YELLOW);
        WorkflowState todo = workflow.addState(Name.of("TODO"), null, ColorType.ANSI_GREEN, StateCategory.INITIAL);
        WorkflowState inProgress =
                workflow.addState(Name.of("IN PROGRESS"), null, ColorType.ANSI_BLUE, StateCategory.ACTIVE);
        WorkflowState done = workflow.addState(Name.of("DONE"), null, ColorType.ANSI_BLACK, StateCategory.COMPLETED);
        workflow.addTransition(Name.of("Start"), null, todo, inProgress);
        workflow.addTransition(Name.of("Complete"), null, inProgress, done);

        workflowRepository.save(workflow);
        workflowId = workflow.getId();

        // setup global issue configuration
        IssueType issueType = IssueType.create(
                Name.of("Story"), null, ColorType.ANSI_RED, IconType.CIRCLE_FILLED, IssueHierarchy.STANDARD, workflow);
        issueTypeRepository.save(issueType);
        issueTypeId = issueType.getId();

        IssueField field1 = issueType.addField(Name.of("goal"), "Goal of the story", IssueFieldType.TEXT, true, 0);
        IssueField field2 =
                issueType.addField(Name.of("release version"), "Release version", IssueFieldType.INTEGER, false, 0);

        em.flush();

        fieldId1 = field1.getId();
        fieldId2 = field2.getId();

        em.clear();
    }

    @Nested
    @DisplayName("create issue")
    class CreateIssue {

        @Test
        @DisplayName("create issue with all fields except sprint and parent issue (common + custom)")
        void successCreateIssueWithAllFields() {
            // given
            Map<Long, Object> customFields = new HashMap<>();
            customFields.put(fieldId1, "Goal is to create the authentication system.");
            customFields.put(fieldId2, 5);

            CreateIssueCommand cmd = CreateIssueCommand.builder()
                    .sprintId(null)
                    .parentProjectKey(null)
                    .parentKey(null)
                    .title("Test Issue")
                    .content("Content")
                    .summary("Summary")
                    .priority(IssuePriority.P2)
                    .dueAt(Instant.now().plus(1, ChronoUnit.DAYS))
                    .storyPoint(10)
                    .issueTypeId(issueTypeId)
                    .customFields(customFields)
                    .assigneeMemberId(member.getId())
                    .build();

            // when
            IssueCreateResponse response = issueLifecycleService.create(PID, cmd, cmd.assigneeMemberId());
            em.flush();
            em.clear();

            // then
            assertThat(response.issueKey()).isEqualTo("PROJ-1");

            Issue issue = issueQueryRepository.findByKey("PROJ-1").orElseThrow();

            assertThat(issue.getTitle()).isEqualTo("Test Issue");
            assertThat(issue.getPriority()).isEqualTo(IssuePriority.P2);
            assertThat(issue.getStoryPoint()).isEqualTo(10);
            assertThat(issue.getCustomFields()).containsKey(String.valueOf(fieldId1));
        }

        @Test
        @DisplayName("fails to create issue if a required custom field is empty")
        void failCreateIssue_If_RequiredCustomFieldEmpty() {
            // given — skip required field 'goal', only provide optional field
            CreateIssueCommand cmd = CreateIssueCommand.builder()
                    .title("Test Issue")
                    .priority(IssuePriority.P2)
                    .issueTypeId(issueTypeId)
                    .customFields(Map.of(fieldId2, 5))
                    .build();

            // when & then
            assertThatThrownBy(() -> issueLifecycleService.create(PID, cmd, member.getId()))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("errorCode")
                    .isEqualTo(IssueErrorCode.CUSTOM_FIELD_REQUIRED);
        }
    }

    @Nested
    @DisplayName("delete issue")
    class DeleteIssue {

        @Test
        @DisplayName("issue is soft-deleted")
        void successIssueSoftDelete() {
            // given
            String issueKey = createBasicIssue();
            IssueIdentifier iid = IssueIdentifier.ofIssueKey(issueKey);

            // when
            issueLifecycleService.delete(iid, member.getId());
            em.flush();
            em.clear();

            // then
            assertThat(issueQueryRepository.findByKey(issueKey)).isEmpty();
            assertThat(issueQueryRepository.findDeletedWithProjectByKey(issueKey))
                    .isPresent();
        }

        @Test
        @DisplayName("fails to delete issue if issue is not in 'INITIAL' state")
        void failIssueDelete_If_NotInitial() {
            // given
            String issueKey = createBasicIssue();

            // transition to 'ACTIVE'
            Issue issue = issueQueryRepository.findWithBasicInfoByKey(issueKey).orElseThrow();
            Workflow workflow = workflowRepository.findById(workflowId).orElseThrow();
            WorkflowState activeState = workflow.getStates().stream()
                    .filter(s -> s.getCategory() == StateCategory.ACTIVE)
                    .findFirst()
                    .orElseThrow();
            issue.transitionTo(activeState);
            em.flush();
            em.clear();

            IssueIdentifier iid = IssueIdentifier.ofIssueKey(issueKey);

            // when & then
            assertThatThrownBy(() -> issueLifecycleService.delete(iid, member.getId()))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("errorCode")
                    .isEqualTo(IssueErrorCode.ISSUE_IN_PROGRESS_DELETION_NOT_ALLOWED);
        }
    }

    private String createBasicIssue() {
        CreateIssueCommand cmd = CreateIssueCommand.builder()
                .title("Test Issue")
                .priority(IssuePriority.P2)
                .issueTypeId(issueTypeId)
                .customFields(Map.of(fieldId1, "test goal"))
                .build();

        IssueCreateResponse response = issueLifecycleService.create(PID, cmd, member.getId());
        em.flush();
        em.clear();
        return response.issueKey();
    }
}

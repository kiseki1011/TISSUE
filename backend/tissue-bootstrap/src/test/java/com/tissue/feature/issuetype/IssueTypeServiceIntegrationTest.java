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
import com.tissue.feature.workflow.application.port.repository.WorkflowRepository;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.feature.workflow.domain.enums.StateCategory;
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

    private Member admin;
    private Long workflowId;

    @BeforeEach
    void setUp() {
        admin = memberRepository.save(Member.createAsAdmin("admin@tissue.com", "admin", "HongGilDong"));

        Workflow workflow = Workflow.create(Name.of("Test Workflow"), null, ColorType.ANSI_YELLOW);
        workflow.addState(Name.of("Open"), null, ColorType.ANSI_GREEN, StateCategory.INITIAL);
        workflow.addState(Name.of("Done"), null, ColorType.ANSI_BLACK, StateCategory.COMPLETED);
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
                    .color(ColorType.ANSI_RED)
                    .icon(IconType.CIRCLE_FILLED)
                    .issueHierarchy(IssueHierarchy.STANDARD)
                    .workflowId(workflowId)
                    .build();

            // when
            IssueTypeResponse response = issueTypeService.create(cmd, admin.getId());
            em.flush();
            em.clear();

            // then
            IssueType issueType = issueTypeRepository
                    .findWithWorkflowById(response.issueTypeId())
                    .orElseThrow();

            assertThat(issueType.getName()).isEqualTo("Bug");
            assertThat(issueType.getColor()).isEqualTo(ColorType.ANSI_RED);
            assertThat(issueType.getIssueHierarchy()).isEqualTo(IssueHierarchy.STANDARD);
            assertThat(issueType.getWorkflow()).isNotNull();
            assertThat(issueType.getWorkflow().getId()).isEqualTo(workflowId);
        }

        @Test
        @DisplayName("fails if issue type name already exists")
        void failIfDuplicateName() {
            // given
            CreateIssueTypeCommand cmd = CreateIssueTypeCommand.builder()
                    .name(Name.of("Bug"))
                    .description(null)
                    .color(ColorType.ANSI_RED)
                    .icon(IconType.CIRCLE_FILLED)
                    .issueHierarchy(IssueHierarchy.STANDARD)
                    .workflowId(workflowId)
                    .build();

            issueTypeService.create(cmd, admin.getId());
            em.flush();

            CreateIssueTypeCommand duplicateCmd = CreateIssueTypeCommand.builder()
                    .name(Name.of("Bug"))
                    .description(null)
                    .color(ColorType.ANSI_BLUE)
                    .icon(IconType.SQUARE_FILLED)
                    .issueHierarchy(IssueHierarchy.STANDARD)
                    .workflowId(workflowId)
                    .build();

            // when & then
            assertThatThrownBy(() -> issueTypeService.create(duplicateCmd, admin.getId()))
                    .isInstanceOf(ResourceConflictException.class)
                    .extracting("errorCode")
                    .isEqualTo(IssueTypeErrorCode.DUPLICATE_ISSUE_TYPE_NAME);
        }
    }
}

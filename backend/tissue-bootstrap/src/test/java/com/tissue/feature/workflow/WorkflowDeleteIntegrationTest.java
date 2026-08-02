package com.tissue.feature.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.feature.issuetype.application.dto.request.CreateIssueTypeCommand;
import com.tissue.feature.issuetype.application.service.IssueTypeService;
import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.workflow.application.port.repository.WorkflowRepository;
import com.tissue.feature.workflow.application.port.usecase.WorkflowCommandUseCase;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.feature.workflow.domain.WorkflowState;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import com.tissue.feature.workflow.domain.exception.WorkflowErrorCode;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.enums.IconType;
import com.tissue.shared.exception.base.ResourceConflictException;
import com.tissue.shared.meta.Evaluation;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import com.tissue.shared.vo.Name;
import com.tissue.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@LLMGenerated(
        model = "claude-opus-4-8",
        llmInvolvement = LLMInvolvement.VIBE_CODED,
        evaluation = Evaluation.NOT_REVIEWED)
@Transactional
class WorkflowDeleteIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private WorkflowCommandUseCase workflowCommandUseCase;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private IssueTypeService issueTypeService;

    @Autowired
    private MemberCommandRepository memberRepository;

    private Member admin;
    private Long workflowId;

    @BeforeEach
    void createDeletableWorkflow() {
        admin = memberRepository.save(Member.createAsAdmin("admin@tissue.com", "admin", "HongGilDong"));

        // A workflow with a transition is the shape that broke the plain cascade. Deleting it made
        // Hibernate try to null workflow_transition.source_state_id (NOT NULL) to break the state FK.
        Workflow workflow = Workflow.create(Name.of("Deletable Flow"), null, ColorType.ANSI_YELLOW);
        WorkflowState open = workflow.addState(Name.of("Open"), null, ColorType.ANSI_GREEN, StateCategory.INITIAL);
        WorkflowState done = workflow.addState(Name.of("Done"), null, ColorType.ANSI_BLACK, StateCategory.COMPLETED);
        workflow.addTransition(Name.of("Finish"), null, open, done);
        workflowRepository.save(workflow);
        workflowId = workflow.getId();

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("deletes a workflow together with its states and transitions")
    void deletesWorkflowWithGraph() {
        // when
        workflowCommandUseCase.delete(workflowId, admin.getId());
        em.flush();
        em.clear();

        // then: the workflow and its whole graph are gone (this threw a NOT-NULL violation before the fix)
        assertThat(workflowRepository.findById(workflowId)).isEmpty();
        assertThat(countRows("workflow_transition")).isZero();
        assertThat(countRows("workflow_state")).isZero();
    }

    @Test
    @DisplayName("refuses to delete a workflow still assigned to an issue type")
    void refusesDeleteWhenAssignedToIssueType() {
        // given: an issue type points at the workflow (issue_type.workflow_id is NOT NULL)
        issueTypeService.create(
                CreateIssueTypeCommand.builder()
                        .name(Name.of("Bug"))
                        .description(null)
                        .color(ColorType.ANSI_RED)
                        .icon(IconType.CIRCLE_FILLED)
                        .issueHierarchy(IssueHierarchy.STANDARD)
                        .workflowId(workflowId)
                        .build(),
                admin.getId());
        em.flush();
        em.clear();

        // when & then: blocked with an accurate in-use conflict, and nothing is deleted
        assertThatThrownBy(() -> workflowCommandUseCase.delete(workflowId, admin.getId()))
                .isInstanceOf(ResourceConflictException.class)
                .extracting("errorCode")
                .isEqualTo(WorkflowErrorCode.WORKFLOW_IN_USE);

        assertThat(workflowRepository.findById(workflowId)).isPresent();
    }

    private long countRows(String table) {
        return ((Number) em.createNativeQuery("SELECT COUNT(*) FROM " + table + " WHERE workflow_id = :id")
                        .setParameter("id", workflowId)
                        .getSingleResult())
                .longValue();
    }
}

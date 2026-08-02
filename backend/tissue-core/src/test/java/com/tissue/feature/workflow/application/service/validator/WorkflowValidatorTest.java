package com.tissue.feature.workflow.application.service.validator;

import static com.tissue.feature.workflow.domain.exception.WorkflowErrorCode.WORKFLOW_IN_USE;
import static com.tissue.feature.workflow.domain.exception.WorkflowErrorCode.WORKFLOW_STATE_IN_USE;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.tissue.feature.issue.application.port.repository.IssueQueryRepository;
import com.tissue.feature.issuetype.application.port.repository.IssueTypeRepository;
import com.tissue.feature.workflow.application.port.repository.WorkflowRepository;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.shared.exception.base.ResourceConflictException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkflowValidatorTest {

    @SuppressWarnings("UnusedVariable")
    @Mock
    private WorkflowRepository workflowQueryRepository;

    @Mock
    private IssueQueryRepository issueRepository;

    @Mock
    private IssueTypeRepository issueTypeRepository;

    @InjectMocks
    private WorkflowValidator sut;

    @Test
    @DisplayName("fail: a workflow still assigned to an issue type is not deletable")
    void notDeletable_whenAssignedToIssueType() {
        // given
        Workflow workflow = org.mockito.Mockito.mock(Workflow.class);
        given(workflow.getId()).willReturn(3L);
        given(issueTypeRepository.existsByWorkflow_Id(3L)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> sut.ensureWorkflowDeletable(workflow))
                .isInstanceOf(ResourceConflictException.class)
                .extracting("errorCode")
                .isEqualTo(WORKFLOW_IN_USE);
        // the issue-type check short-circuits before the per-state active-issue query
        then(issueRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("fail: a workflow whose states hold active issues is not deletable")
    void notDeletable_whenStatesHoldActiveIssues() {
        // given
        Workflow workflow = org.mockito.Mockito.mock(Workflow.class);
        given(workflow.getId()).willReturn(3L);
        given(issueTypeRepository.existsByWorkflow_Id(3L)).willReturn(false);
        given(workflow.getStates()).willReturn(List.of());
        given(issueRepository.findStateIdsUsedByActiveIssues(anyList())).willReturn(List.of(9L));

        // when & then
        assertThatThrownBy(() -> sut.ensureWorkflowDeletable(workflow))
                .isInstanceOf(ResourceConflictException.class)
                .extracting("errorCode")
                .isEqualTo(WORKFLOW_STATE_IN_USE);
    }

    @Test
    @DisplayName("success: an unreferenced workflow with no active-issue states is deletable")
    void deletable_whenUnreferencedAndUnused() {
        // given
        Workflow workflow = org.mockito.Mockito.mock(Workflow.class);
        given(workflow.getId()).willReturn(3L);
        given(issueTypeRepository.existsByWorkflow_Id(3L)).willReturn(false);
        given(workflow.getStates()).willReturn(List.of());
        given(issueRepository.findStateIdsUsedByActiveIssues(anyList())).willReturn(List.of());

        // when & then
        assertThatCode(() -> sut.ensureWorkflowDeletable(workflow)).doesNotThrowAnyException();
    }
}

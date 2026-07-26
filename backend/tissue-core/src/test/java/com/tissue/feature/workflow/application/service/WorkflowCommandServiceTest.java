package com.tissue.feature.workflow.application.service;

import static com.tissue.feature.workflow.domain.exception.WorkflowErrorCode.WORKFLOW_IN_USE;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.tissue.feature.workflow.application.port.repository.WorkflowDeleteRepository;
import com.tissue.feature.workflow.application.port.repository.WorkflowRepository;
import com.tissue.feature.workflow.application.service.finder.WorkflowFinder;
import com.tissue.feature.workflow.application.service.validator.WorkflowGraphValidator;
import com.tissue.feature.workflow.application.service.validator.WorkflowValidator;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.feature.workflow.domain.service.TransitionGuardRegistry;
import com.tissue.shared.exception.base.ResourceConflictException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkflowCommandServiceTest {

    @Mock
    private WorkflowFinder workflowFinder;

    @Mock
    private WorkflowRepository workflowRepository;

    @Mock
    private WorkflowDeleteRepository workflowDeleteRepository;

    @Mock
    private WorkflowValidator workflowValidator;

    @SuppressWarnings("UnusedVariable")
    @Mock
    private WorkflowGraphValidator graphValidator;

    @SuppressWarnings("UnusedVariable")
    @Mock
    private TransitionGuardRegistry guardRegistry;

    @InjectMocks
    private WorkflowCommandService sut;

    @Nested
    @DisplayName("delete workflow")
    class DeleteWorkflow {

        @Test
        @DisplayName("success: purges the aggregate child-to-parent in FK-safe order, not via a plain cascade")
        void deletesChildrenInFkSafeOrder() {
            // given
            Long workflowId = 3L;
            Workflow workflow = mock(Workflow.class);
            given(workflowFinder.getById(workflowId)).willReturn(workflow);

            // when
            sut.delete(workflowId, 1L);

            // then: deletable check first, then guards -> transitions -> detach initial -> states -> workflow
            InOrder ordered = inOrder(workflowValidator, workflowDeleteRepository);
            ordered.verify(workflowValidator).ensureWorkflowDeletable(workflow);
            ordered.verify(workflowDeleteRepository).deleteGuardConfigs(workflowId);
            ordered.verify(workflowDeleteRepository).deleteTransitions(workflowId);
            ordered.verify(workflowDeleteRepository).detachInitialState(workflowId);
            ordered.verify(workflowDeleteRepository).deleteStates(workflowId);
            ordered.verify(workflowDeleteRepository).deleteWorkflow(workflowId);
            // the buggy JPA cascade path must not be used
            then(workflowRepository).should(never()).delete(any());
        }

        @Test
        @DisplayName("fail: an in-use workflow is not purged")
        void inUseWorkflowIsNotPurged() {
            // given
            Long workflowId = 3L;
            Workflow workflow = mock(Workflow.class);
            given(workflowFinder.getById(workflowId)).willReturn(workflow);
            willThrow(new ResourceConflictException(WORKFLOW_IN_USE))
                    .given(workflowValidator)
                    .ensureWorkflowDeletable(workflow);

            // when & then
            assertThatThrownBy(() -> sut.delete(workflowId, 1L))
                    .isInstanceOf(ResourceConflictException.class)
                    .extracting("errorCode")
                    .isEqualTo(WORKFLOW_IN_USE);
            then(workflowDeleteRepository).shouldHaveNoInteractions();
        }
    }
}

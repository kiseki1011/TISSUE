package com.tissue.feature.workflow.application.service.validator;

import static com.tissue.feature.workflow.domain.enums.StateCategory.ACTIVE;
import static com.tissue.feature.workflow.domain.enums.StateCategory.COMPLETED;
import static com.tissue.feature.workflow.domain.enums.StateCategory.INITIAL;
import static com.tissue.feature.workflow.domain.exception.WorkflowErrorCode.INVALID_INITIAL_STATE_COUNT;
import static com.tissue.feature.workflow.domain.exception.WorkflowErrorCode.INVALID_TRANSITION_TARGET;
import static com.tissue.feature.workflow.domain.exception.WorkflowErrorCode.MISSING_COMPLETED_STATE;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.project.domain.Project;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.feature.workflow.domain.WorkflowState;
import com.tissue.feature.workflow.domain.exception.DeadEndStateException;
import com.tissue.feature.workflow.domain.exception.OrphanStateException;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.exception.base.BadRequestException;
import com.tissue.shared.vo.Name;
import com.tissue.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WorkflowGraphValidatorTest {

    private final WorkflowGraphValidator validator = new WorkflowGraphValidator();

    @Test
    @DisplayName("fail: throws OrphanStateException if orphan state not reachable from initial")
    void fail_When_OrphanStateExists() {
        // given
        Workspace workspace = TestFixtures.workspace("WORKSPACE");
        Project project = TestFixtures.project(workspace, "PROJ");
        Workflow wf = TestFixtures.workflow(project);

        WorkflowState initial = wf.addState(Name.of("Open"), null, ColorType.BLUE, INITIAL);
        WorkflowState active = wf.addState(Name.of("In Progress"), null, ColorType.YELLOW, ACTIVE);
        wf.addState(Name.of("Orphan"), null, ColorType.RED, ACTIVE);
        WorkflowState done = wf.addState(Name.of("Done"), null, ColorType.GREEN, COMPLETED);

        wf.addTransition(Name.of("Start"), null, initial, active);
        wf.addTransition(Name.of("Finish"), null, active, done);

        // when & then
        assertThatThrownBy(() -> validator.ensureValidWorkflowGraph(wf)).isInstanceOf(OrphanStateException.class);
    }

    @Test
    @DisplayName("fail: throws BadRequestException if more than one 'INITIAL' state exists")
    void fail_When_MultipleInitialStateExists() {
        // given
        Workspace workspace = TestFixtures.workspace("WORKSPACE");
        Project project = TestFixtures.project(workspace, "PROJ");
        Workflow wf = TestFixtures.workflow(project);

        WorkflowState initial = wf.addState(Name.of("Open"), null, ColorType.BLUE, INITIAL);
        WorkflowState initial2 = wf.addState(Name.of("Todo"), null, ColorType.BLUE, INITIAL);
        WorkflowState active = wf.addState(Name.of("In Progress"), null, ColorType.YELLOW, ACTIVE);
        WorkflowState done = wf.addState(Name.of("Done"), null, ColorType.GREEN, COMPLETED);

        wf.addTransition(Name.of("Start"), null, initial, active);
        wf.addTransition(Name.of("Start 2"), null, initial2, active);
        wf.addTransition(Name.of("Finish"), null, active, done);

        // when & then
        assertThatThrownBy(() -> validator.ensureValidWorkflowGraph(wf))
                .isInstanceOf(BadRequestException.class)
                .extracting("errorCode")
                .isEqualTo(INVALID_INITIAL_STATE_COUNT);
    }

    @Test
    @DisplayName("fail: throws exception if 'COMPLETED' state doesn't exist")
    void fail_When_CompletedState_Not_Exist() {
        // given
        Workspace workspace = TestFixtures.workspace("WORKSPACE");
        Project project = TestFixtures.project(workspace, "PROJ");
        Workflow wf = TestFixtures.workflow(project);

        WorkflowState initial = wf.addState(Name.of("Open"), null, ColorType.BLUE, INITIAL);
        WorkflowState active = wf.addState(Name.of("In Progress"), null, ColorType.YELLOW, ACTIVE);

        wf.addTransition(Name.of("Start"), null, initial, active);

        // when & then
        assertThatThrownBy(() -> validator.ensureValidWorkflowGraph(wf))
                .isInstanceOf(BadRequestException.class)
                .extracting("errorCode")
                .isEqualTo(MISSING_COMPLETED_STATE);
    }

    @Test
    @DisplayName("fail: throws exception if a transition target is an 'INITIAL' state")
    void fail_When_TransitionTarget_Is_InitialState() {
        // given
        Workspace workspace = TestFixtures.workspace("WORKSPACE");
        Project project = TestFixtures.project(workspace, "PROJ");
        Workflow wf = TestFixtures.workflow(project);

        WorkflowState initial = wf.addState(Name.of("Open"), null, ColorType.BLUE, INITIAL);
        WorkflowState active = wf.addState(Name.of("In Progress"), null, ColorType.YELLOW, ACTIVE);
        WorkflowState done = wf.addState(Name.of("Done"), null, ColorType.GREEN, COMPLETED);

        wf.addTransition(Name.of("Start"), null, initial, active);
        wf.addTransition(Name.of("Finish"), null, active, done);
        wf.addTransition(Name.of("Into Initial"), null, active, initial);

        // when & then
        assertThatThrownBy(() -> validator.ensureValidWorkflowGraph(wf))
                .isInstanceOf(BadRequestException.class)
                .extracting("errorCode")
                .isEqualTo(INVALID_TRANSITION_TARGET);
    }

    @Test
    @DisplayName("fail: throws DeadEndStateException if 'ACTIVE' state doesn't have an outgoing transition")
    void fail_When_ActiveState_No_OutgoingTransition() {
        // given
        Workspace workspace = TestFixtures.workspace("WORKSPACE");
        Project project = TestFixtures.project(workspace, "PROJ");
        Workflow wf = TestFixtures.workflow(project);

        WorkflowState initial = wf.addState(Name.of("Open"), null, ColorType.BLUE, INITIAL);
        WorkflowState active = wf.addState(Name.of("In Progress"), null, ColorType.YELLOW, ACTIVE);
        WorkflowState deadEnd = wf.addState(Name.of("Dead End"), null, ColorType.RED, ACTIVE);
        WorkflowState done = wf.addState(Name.of("Done"), null, ColorType.GREEN, COMPLETED);

        wf.addTransition(Name.of("Start"), null, initial, active);
        wf.addTransition(Name.of("To Dead End"), null, active, deadEnd);
        wf.addTransition(Name.of("Finish"), null, active, done);

        // when & then
        assertThatThrownBy(() -> validator.ensureValidWorkflowGraph(wf)).isInstanceOf(DeadEndStateException.class);
    }
}

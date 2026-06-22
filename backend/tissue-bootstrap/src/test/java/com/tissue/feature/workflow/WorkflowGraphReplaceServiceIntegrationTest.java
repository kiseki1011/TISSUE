package com.tissue.feature.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.workflow.application.dto.CreateStateDefinition;
import com.tissue.feature.workflow.application.dto.CreateTransitionDefinition;
import com.tissue.feature.workflow.application.dto.NodeIdentifier;
import com.tissue.feature.workflow.application.dto.StateDefinition;
import com.tissue.feature.workflow.application.dto.TransitionDefinition;
import com.tissue.feature.workflow.application.dto.request.CreateWorkflowCommand;
import com.tissue.feature.workflow.application.dto.request.ReplaceWorkflowGraphCommand;
import com.tissue.feature.workflow.application.dto.response.WorkflowCreateResponse;
import com.tissue.feature.workflow.application.port.repository.WorkflowRepository;
import com.tissue.feature.workflow.application.service.WorkflowCommandService;
import com.tissue.feature.workflow.application.service.WorkflowGraphReplaceService;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.feature.workflow.domain.WorkflowState;
import com.tissue.feature.workflow.domain.WorkflowTransition;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import com.tissue.feature.workflow.domain.exception.WorkflowVersionMismatchException;
import com.tissue.shared.enums.ColorType;
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
class WorkflowGraphReplaceServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private WorkflowGraphReplaceService workflowGraphReplaceService;

    @Autowired
    private WorkflowCommandService workflowCommandService;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private MemberCommandRepository memberRepository;

    private Member admin;

    @BeforeEach
    void setUp() {
        admin = memberRepository.save(Member.createAsAdmin("admin@tissue.com", "admin", "HongGilDong"));
        em.flush();
    }

    /**
     * Creates a workflow: Open (INITIAL) → In Progress (ACTIVE) → Done (COMPLETED)
     */
    private Workflow createWorkflow() {
        CreateWorkflowCommand cmd = CreateWorkflowCommand.builder()
                .name(Name.of("Test Workflow"))
                .color(ColorType.ANSI_YELLOW)
                .stateDefinitions(List.of(
                        new CreateStateDefinition(
                                "s1", Name.of("Open"), null, ColorType.ANSI_GREEN, StateCategory.INITIAL),
                        new CreateStateDefinition(
                                "s2", Name.of("In Progress"), null, ColorType.ANSI_BLUE, StateCategory.ACTIVE),
                        new CreateStateDefinition(
                                "s3", Name.of("Done"), null, ColorType.ANSI_BLACK, StateCategory.COMPLETED)))
                .transitionDefinitions(List.of(
                        new CreateTransitionDefinition(Name.of("Start"), null, "s1", "s2"),
                        new CreateTransitionDefinition(Name.of("Complete"), null, "s2", "s3")))
                .build();

        WorkflowCreateResponse created = workflowCommandService.create(cmd, admin.getId());
        em.flush();
        em.clear();

        return workflowRepository.findById(created.workflowId()).orElseThrow();
    }

    @Nested
    @DisplayName("replace workflow graph")
    class ReplaceWorkflowGraph {

        @Test
        @DisplayName("replaces graph by adding new state, removing old state and rewiring transitions")
        void replaceGraphSuccessfully() {
            // given
            Workflow workflow = createWorkflow();

            WorkflowState open = workflow.getStates().stream()
                    .filter(s -> s.getDisplayName().equals("Open"))
                    .findFirst()
                    .orElseThrow();
            WorkflowState done = workflow.getStates().stream()
                    .filter(s -> s.getDisplayName().equals("Done"))
                    .findFirst()
                    .orElseThrow();
            WorkflowTransition startTransition = workflow.getTransitions().stream()
                    .filter(t -> t.getDisplayName().equals("Start"))
                    .findFirst()
                    .orElseThrow();

            // replace: Open → Review (new) → Done, remove "In Progress"
            ReplaceWorkflowGraphCommand replaceCmd = new ReplaceWorkflowGraphCommand(
                    workflow.getVersion(),
                    List.of(
                            new StateDefinition(
                                    new NodeIdentifier.ExistingId(open.getId()),
                                    null,
                                    null,
                                    null,
                                    StateCategory.INITIAL),
                            new StateDefinition(
                                    new NodeIdentifier.TempKey("review"),
                                    Name.of("Review"),
                                    null,
                                    ColorType.ANSI_YELLOW,
                                    StateCategory.ACTIVE),
                            new StateDefinition(
                                    new NodeIdentifier.ExistingId(done.getId()),
                                    null,
                                    null,
                                    null,
                                    StateCategory.COMPLETED)),
                    List.of(
                            new TransitionDefinition(
                                    new NodeIdentifier.ExistingId(startTransition.getId()),
                                    null,
                                    null,
                                    new NodeIdentifier.ExistingId(open.getId()),
                                    new NodeIdentifier.TempKey("review")),
                            new TransitionDefinition(
                                    new NodeIdentifier.TempKey("t-approve"),
                                    Name.of("Approve"),
                                    null,
                                    new NodeIdentifier.TempKey("review"),
                                    new NodeIdentifier.ExistingId(done.getId()))),
                    List.of());

            // when
            workflowGraphReplaceService.replaceWorkflowGraph(workflow.getId(), replaceCmd, admin.getId());
            em.flush();
            em.clear();

            // then
            Workflow reloaded = workflowRepository.findById(workflow.getId()).orElseThrow();

            assertThat(reloaded.getStates()).hasSize(3);
            assertThat(reloaded.getStates())
                    .extracting(WorkflowState::getDisplayName)
                    .containsExactlyInAnyOrder("Open", "Review", "Done");

            assertThat(reloaded.getTransitions()).hasSize(2);
            assertThat(reloaded.getTransitions())
                    .extracting(WorkflowTransition::getDisplayName)
                    .containsExactlyInAnyOrder("Start", "Approve");

            assertThat(reloaded.getInitialState().getDisplayName()).isEqualTo("Open");
        }

        @Test
        @DisplayName("fails if workflow version does not match")
        void failIfVersionMismatch() {
            // given
            Workflow workflow = createWorkflow();

            WorkflowState open = workflow.getStates().stream()
                    .filter(s -> s.getDisplayName().equals("Open"))
                    .findFirst()
                    .orElseThrow();
            WorkflowState done = workflow.getStates().stream()
                    .filter(s -> s.getDisplayName().equals("Done"))
                    .findFirst()
                    .orElseThrow();

            Long wrongVersion = workflow.getVersion() + 999;

            ReplaceWorkflowGraphCommand replaceCmd = new ReplaceWorkflowGraphCommand(
                    wrongVersion,
                    List.of(
                            new StateDefinition(
                                    new NodeIdentifier.ExistingId(open.getId()),
                                    null,
                                    null,
                                    null,
                                    StateCategory.INITIAL),
                            new StateDefinition(
                                    new NodeIdentifier.ExistingId(done.getId()),
                                    null,
                                    null,
                                    null,
                                    StateCategory.COMPLETED)),
                    List.of(new TransitionDefinition(
                            new NodeIdentifier.TempKey("t1"),
                            Name.of("Finish"),
                            null,
                            new NodeIdentifier.ExistingId(open.getId()),
                            new NodeIdentifier.ExistingId(done.getId()))),
                    List.of());

            // when & then
            assertThatThrownBy(() -> workflowGraphReplaceService.replaceWorkflowGraph(
                            workflow.getId(), replaceCmd, admin.getId()))
                    .isInstanceOf(WorkflowVersionMismatchException.class);
        }
    }
}

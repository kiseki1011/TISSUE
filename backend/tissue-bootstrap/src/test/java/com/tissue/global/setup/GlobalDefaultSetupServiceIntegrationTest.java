package com.tissue.global.setup;

import static org.assertj.core.api.Assertions.assertThat;

import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.feature.issuetype.application.port.repository.IssueTypeRepository;
import com.tissue.feature.issuetype.domain.IssueField;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.workflow.application.port.repository.WorkflowRepository;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.feature.workflow.domain.WorkflowTransition;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import com.tissue.feature.workflow.domain.guard.GuardType;
import com.tissue.support.IntegrationTestSupport;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class GlobalDefaultSetupServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private GlobalDefaultSetupService globalDefaultSetupService;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private IssueTypeRepository issueTypeRepository;

    @BeforeEach
    void setUp() {
        globalDefaultSetupService.setupDefaults();
        em.flush();
        em.clear();
    }

    @Nested
    @DisplayName("default workflows")
    class DefaultWorkflows {

        @Test
        @DisplayName("creates the two system-provided default workflows")
        void createsDefaultWorkflows() {
            // when
            List<Workflow> workflows = workflowRepository.findAllByOrderByName();

            // then
            assertThat(workflows).hasSize(2);
            assertThat(workflows)
                    .allSatisfy(wf -> assertThat(wf.isSystemProvided()).isTrue());

            Workflow reviewWorkflow = workflows.stream()
                    .filter(wf -> wf.getName().equals("Review Workflow"))
                    .findFirst()
                    .orElseThrow();
            assertThat(reviewWorkflow.getStates()).hasSize(5);
            assertThat(reviewWorkflow.getTransitions()).hasSize(6);
            assertThat(reviewWorkflow.getInitialState().getDisplayName()).isEqualTo("To Do");
            assertThat(reviewWorkflow.getStates().stream()
                            .filter(s -> s.isCategorizedAs(StateCategory.COMPLETED))
                            .count())
                    .isEqualTo(1);

            Workflow basicWorkflow = workflows.stream()
                    .filter(wf -> wf.getName().equals("Basic Workflow"))
                    .findFirst()
                    .orElseThrow();
            assertThat(basicWorkflow.getStates()).hasSize(4);
            assertThat(basicWorkflow.getTransitions()).hasSize(3);
        }

        @Test
        @DisplayName("review workflow approve transition has REQUIRED_APPROVAL guard")
        void reviewWorkflowHasApprovalGuard() {
            // when
            Workflow reviewWorkflow = workflowRepository.findAllByOrderByName().stream()
                    .filter(wf -> wf.getName().equals("Review Workflow"))
                    .findFirst()
                    .orElseThrow();

            WorkflowTransition approveTransition = reviewWorkflow.getTransitions().stream()
                    .filter(t -> t.getDisplayName().equals("Approve"))
                    .findFirst()
                    .orElseThrow();

            // then
            assertThat(approveTransition.getGuardConfigs()).hasSize(1);
            assertThat(approveTransition.getGuardConfigs().getFirst().getGuardType())
                    .isEqualTo(GuardType.REQUIRED_APPROVAL);
        }
    }

    @Nested
    @DisplayName("default issue types")
    class DefaultIssueTypes {

        @Test
        @DisplayName("creates the six system-provided default issue types")
        void createsDefaultIssueTypes() {
            // when
            List<IssueType> issueTypes = issueTypeRepository.findAllWithWorkflow();

            // then
            assertThat(issueTypes).hasSize(6);
            assertThat(issueTypes)
                    .allSatisfy(it -> assertThat(it.isSystemProvided()).isTrue());

            assertThat(issueTypes)
                    .extracting(IssueType::getName)
                    .containsExactlyInAnyOrder("Epic", "Story", "Task", "Bug", "Sub Task", "Micro Task");

            assertThat(issueTypes)
                    .extracting(IssueType::getIssueHierarchy)
                    .containsExactlyInAnyOrder(
                            IssueHierarchy.EPIC,
                            IssueHierarchy.STANDARD,
                            IssueHierarchy.STANDARD,
                            IssueHierarchy.STANDARD,
                            IssueHierarchy.SUBTASK,
                            IssueHierarchy.MICROTASK);
        }

        @Test
        @DisplayName("bug issue type has custom fields")
        void bugIssueTypeHasCustomFields() {
            // when
            IssueType bug = issueTypeRepository.findAllWithWorkflow().stream()
                    .filter(it -> it.getName().equals("Bug"))
                    .findFirst()
                    .orElseThrow();

            // then
            assertThat(bug.getFields()).hasSize(3);
            assertThat(bug.getFields())
                    .extracting(IssueField::getName)
                    .containsExactly("reproduceSteps", "environment", "version");
        }
    }
}

package com.tissue.global.setup;

import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.feature.issuetype.application.port.repository.IssueTypeRepository;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.issuetype.domain.enums.IssueFieldType;
import com.tissue.feature.workflow.application.port.repository.WorkflowRepository;
import com.tissue.feature.workflow.application.service.validator.WorkflowGraphValidator;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.feature.workflow.domain.WorkflowState;
import com.tissue.feature.workflow.domain.WorkflowTransition;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import com.tissue.feature.workflow.domain.guard.GuardType;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.enums.IconType;
import com.tissue.shared.vo.Name;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Seeds the instance-wide default workflows and issue types.
 *
 * <p>Workflows and issue types are managed globally (not per project). These defaults are created
 * once, when the first member signs up on a fresh instance.
 */
// TODO: use i18n messages for the name and descriptions?
// TODO: add more workflows
//  (for agentic workflow, need to add more transitions guards)
// TODO: add/refine issue types
@Component
@RequiredArgsConstructor
public class GlobalDefaultSetupService {

    private final WorkflowRepository workflowRepository;
    private final IssueTypeRepository issueTypeRepository;
    private final WorkflowGraphValidator workflowGraphValidator;

    public void setupDefaults() {
        Workflow reviewWorkflow = createReviewWorkflow();
        Workflow basicWorkflow = createBasicWorkflow();

        workflowGraphValidator.ensureValidWorkflowGraph(reviewWorkflow);
        workflowGraphValidator.ensureValidWorkflowGraph(basicWorkflow);

        workflowRepository.save(reviewWorkflow);
        workflowRepository.save(basicWorkflow);

        createDefaultIssueTypes(reviewWorkflow, basicWorkflow);
    }

    private Workflow createReviewWorkflow() {
        Workflow wf =
                Workflow.create(Name.of("Review Workflow"), "Default workflow with review stage", ColorType.PURPLE);

        WorkflowState toDo = wf.addState(Name.of("To Do"), null, ColorType.GRAY, StateCategory.INITIAL);
        WorkflowState inProgress = wf.addState(Name.of("In Progress"), null, ColorType.BLUE, StateCategory.ACTIVE);
        WorkflowState inReview = wf.addState(Name.of("In Review"), null, ColorType.YELLOW, StateCategory.ACTIVE);
        WorkflowState done = wf.addState(Name.of("Done"), null, ColorType.GREEN, StateCategory.COMPLETED);
        WorkflowState cancelled = wf.addState(Name.of("Cancelled"), null, ColorType.RED, StateCategory.ABORTED);

        wf.addTransition(Name.of("Start"), null, toDo, inProgress);
        wf.addTransition(Name.of("Request Review"), null, inProgress, inReview);
        wf.addTransition(Name.of("Approve"), null, inReview, done);
        wf.addTransition(Name.of("Reject"), null, inReview, inProgress);
        wf.addTransition(Name.of("Cancel"), null, inProgress, cancelled);
        wf.addTransition(Name.of("Cancel"), null, inReview, cancelled);

        WorkflowTransition approveTransition = findTransitionByName(wf, "Approve");
        wf.addTransitionGuard(
                approveTransition,
                GuardType.APPROVAL_REQUIRED,
                Map.of(
                        "min_approvals",
                        1,
                        "block_on_change_request",
                        true,
                        "auto_transition_on_reject",
                        true,
                        "reject_transition_name",
                        "Reject"),
                1);

        wf.setAsSystemProvided();
        return wf;
    }

    private Workflow createBasicWorkflow() {
        Workflow wf = Workflow.create(Name.of("Basic Workflow"), "Default simple workflow", ColorType.BLUE);

        WorkflowState toDo = wf.addState(Name.of("To Do"), null, ColorType.GRAY, StateCategory.INITIAL);
        WorkflowState inProgress = wf.addState(Name.of("In Progress"), null, ColorType.BLUE, StateCategory.ACTIVE);
        WorkflowState done = wf.addState(Name.of("Done"), null, ColorType.GREEN, StateCategory.COMPLETED);
        WorkflowState cancelled = wf.addState(Name.of("Cancelled"), null, ColorType.RED, StateCategory.ABORTED);

        wf.addTransition(Name.of("Start"), null, toDo, inProgress);
        wf.addTransition(Name.of("Finish"), null, inProgress, done);
        wf.addTransition(Name.of("Cancel"), null, inProgress, cancelled);

        wf.setAsSystemProvided();
        return wf;
    }

    private void createDefaultIssueTypes(Workflow reviewWorkflow, Workflow basicWorkflow) {
        IssueType epic = IssueType.create(
                Name.of("Epic"),
                "Track large initiatives",
                ColorType.PURPLE,
                IconType.DIAMOND_FILLED,
                IssueHierarchy.EPIC,
                basicWorkflow);

        epic.addField(Name.of("goal"), "Epic goal", IssueFieldType.TEXT, false, 0);
        epic.setAsSystemProvided();
        issueTypeRepository.save(epic);

        IssueType story = IssueType.create(
                Name.of("Story"),
                "User story",
                ColorType.GREEN,
                IconType.CIRCLE_FILLED,
                IssueHierarchy.STANDARD,
                reviewWorkflow);
        story.addField(Name.of("story"), "User story description", IssueFieldType.TEXT, false, 0);
        story.setAsSystemProvided();
        issueTypeRepository.save(story);

        IssueType task = IssueType.create(
                Name.of("Task"),
                "General task",
                ColorType.BLUE,
                IconType.SQUARE_FILLED,
                IssueHierarchy.STANDARD,
                basicWorkflow);
        task.setAsSystemProvided();
        issueTypeRepository.save(task);

        IssueType bug = IssueType.create(
                Name.of("Bug"), "Bug report", ColorType.RED, IconType.WARNING, IssueHierarchy.STANDARD, reviewWorkflow);
        bug.addField(Name.of("reproduceSteps"), "Steps to reproduce", IssueFieldType.TEXT, false, 0);
        bug.addField(Name.of("environment"), "Environment details", IssueFieldType.TEXT, false, 1);
        bug.addField(Name.of("version"), "Affected version", IssueFieldType.TEXT, false, 2);
        bug.setAsSystemProvided();
        issueTypeRepository.save(bug);

        IssueType subTask = IssueType.create(
                Name.of("Sub Task"),
                "Subtask",
                ColorType.CYAN,
                IconType.CIRCLE_OUTLINE,
                IssueHierarchy.SUBTASK,
                basicWorkflow);
        subTask.setAsSystemProvided();
        issueTypeRepository.save(subTask);

        IssueType microTask = IssueType.create(
                Name.of("Micro Task"),
                "Microtask",
                ColorType.GRAY,
                IconType.CIRCLE_DOT,
                IssueHierarchy.MICROTASK,
                basicWorkflow);
        microTask.setAsSystemProvided();
        issueTypeRepository.save(microTask);
    }

    private WorkflowTransition findTransitionByName(Workflow workflow, String name) {
        return workflow.getTransitions().stream()
                .filter(t -> t.getDisplayName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Default transition '%s' not found in workflow '%s'".formatted(name, workflow.getName())));
    }
}

package com.tissue.workflow.domain;

import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

@Embeddable
@Getter
public class VcsAutomationSettings {

    @Nullable
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vcs_pr_opened_transition_id")
    private WorkflowTransition vcsPrOpenedTransition;

    @Nullable
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vcs_pr_merged_transition_id")
    private WorkflowTransition vcsPrMergedTransition;

    @SuppressWarnings("NullAway.Init")
    protected VcsAutomationSettings() {}

    public static VcsAutomationSettings of(
            Workflow workflow,
            @Nullable WorkflowTransition prOpenedTransition,
            @Nullable WorkflowTransition prMergedTransition) {

        VcsAutomationSettings vcsAutomationSettings = new VcsAutomationSettings();
        if (prOpenedTransition != null) {
            vcsAutomationSettings.validateTransitionBelongsToWorkflow(workflow, prOpenedTransition);
        }
        if (prMergedTransition != null) {
            vcsAutomationSettings.validateTransitionBelongsToWorkflow(workflow, prMergedTransition);
        }
        vcsAutomationSettings.vcsPrOpenedTransition = prOpenedTransition;
        vcsAutomationSettings.vcsPrMergedTransition = prMergedTransition;

        return vcsAutomationSettings;
    }

    private void validateTransitionBelongsToWorkflow(Workflow workflowm, WorkflowTransition transition) {
        if (!workflowm.getTransitions().contains(transition)) {
            throw new IllegalArgumentException("Transition does not belong to this workflow");
        }
    }
}

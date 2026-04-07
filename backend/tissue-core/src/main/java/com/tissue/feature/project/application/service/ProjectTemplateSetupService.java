package com.tissue.feature.project.application.service;

import com.tissue.feature.issuetype.application.port.repository.IssueTypeRepository;
import com.tissue.feature.issuetype.domain.IssueField;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.projecttemplate.application.port.repository.ProjectTemplateRepository;
import com.tissue.feature.projecttemplate.domain.ProjectTemplate;
import com.tissue.feature.projecttemplate.domain.config.TemplateConfig;
import com.tissue.feature.projecttemplate.domain.config.TemplateIssueField;
import com.tissue.feature.projecttemplate.domain.config.TemplateIssueType;
import com.tissue.feature.projecttemplate.domain.config.TemplateState;
import com.tissue.feature.projecttemplate.domain.config.TemplateTransition;
import com.tissue.feature.projecttemplate.domain.config.TemplateTransitionGuard;
import com.tissue.feature.projecttemplate.domain.config.TemplateWorkflow;
import com.tissue.feature.projecttemplate.domain.exception.ProjectTemplateNotFoundException;
import com.tissue.feature.workflow.application.port.repository.WorkflowRepository;
import com.tissue.feature.workflow.application.service.validator.WorkflowGraphValidator;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.feature.workflow.domain.WorkflowState;
import com.tissue.feature.workflow.domain.WorkflowTransition;
import com.tissue.shared.vo.Name;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectTemplateSetupService {

    private final ProjectTemplateRepository projectTemplateRepository;
    private final WorkflowRepository workflowRepository;
    private final IssueTypeRepository issueTypeRepository;
    private final WorkflowGraphValidator workflowGraphValidator;

    void setupFromTemplate(Project project, Long templateId) {
        ProjectTemplate template = projectTemplateRepository
                .findById(templateId)
                .orElseThrow(() -> new ProjectTemplateNotFoundException(templateId));

        TemplateConfig config = template.getConfigPayload();

        Map<String, Workflow> workflowMap = new HashMap<>();

        for (TemplateWorkflow tw : config.workflows()) {
            Workflow wf = Workflow.create(project, Name.of(tw.name()), tw.description(), tw.color());

            Map<String, WorkflowState> stateMap = new HashMap<>();

            for (TemplateState ts : tw.states()) {
                WorkflowState state = wf.addState(Name.of(ts.name()), ts.description(), ts.color(), ts.category());
                stateMap.put(ts.name(), state);
            }

            for (TemplateTransition tt : tw.transitions()) {
                WorkflowState source = Objects.requireNonNull(
                        stateMap.get(tt.sourceStateName()), "Source state not found: " + tt.sourceStateName());
                WorkflowState target = Objects.requireNonNull(
                        stateMap.get(tt.targetStateName()), "Target state not found: " + tt.targetStateName());
                wf.addTransition(Name.of(tt.name()), tt.description(), source, target);

                if (!tt.guards().isEmpty()) {
                    WorkflowTransition transition = findLastTransition(wf);
                    for (TemplateTransitionGuard tg : tt.guards()) {
                        wf.addTransitionGuard(transition, tg.guardType(), tg.guardParams(), tg.executionOrder());
                    }
                }
            }

            workflowGraphValidator.ensureValidWorkflowGraph(wf);
            workflowRepository.save(wf);

            workflowMap.put(tw.tempId(), wf);
        }

        for (TemplateIssueType tit : config.issueTypes()) {
            Workflow linkedWorkflow =
                    Objects.requireNonNull(workflowMap.get(tit.workflowTempId()), "Workflow not found");

            IssueType issueType = IssueType.create(
                    project,
                    Name.of(tit.name()),
                    tit.description(),
                    tit.color(),
                    tit.icon(),
                    tit.hierarchy(),
                    linkedWorkflow);

            for (TemplateIssueField tif : tit.fields()) {
                IssueField field = issueType.addField(
                        Name.of(tif.name()), tif.description(), tif.type(), tif.required(), tif.position());

                for (String optionName : tif.options()) {
                    field.addOption(Name.of(optionName));
                }
            }

            issueTypeRepository.save(issueType);
        }
    }

    private WorkflowTransition findLastTransition(Workflow wf) {
        List<WorkflowTransition> transitions = wf.getTransitions();
        return transitions.getLast();
    }
}

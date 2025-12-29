package com.tissue.workflow.adapter.in.web.dto.request;

import com.tissue.workflow.application.dto.EntityRef;
import com.tissue.workflow.application.dto.StateDefinition;
import com.tissue.workflow.application.dto.TransitionDefinition;
import com.tissue.workflow.application.dto.request.ReplaceWorkflowGraphCommand;
import com.tissue.workflow.domain.enums.StateCategory;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * TODO: 자세한 문서화 필요 - 기존 status, transition은 id 전달 - 새로 추가되는 status, transition은 tempKey 전달 -
 * tempKey는 클라이언트에서 생성. UUID 계열 권장(원하면 transliteration 사용)
 */
public record ReplaceWorkflowGraphRequest(
        @NotNull Long version,
        @NotEmpty List<ReplaceStatusRequest> replaceStatusRequests,
        @NotEmpty List<ReplaceTransitionRequest> replaceTransitionRequests) {
    public record ReplaceStatusRequest(
            Long id, String tempKey, @NotNull StateCategory category) {}

    public record ReplaceTransitionRequest(
            Long id,
            String tempKey,
            @NotNull EntityRef source,
            @NotNull EntityRef target) {}

    public ReplaceWorkflowGraphCommand toCommand(String workspaceKey, String projectKey, Long workflowId) {
        return new ReplaceWorkflowGraphCommand(
                workspaceKey,
                projectKey,
                workflowId,
                version,
                replaceStatusRequests.stream()
                        .map(s -> StateDefinition.builder()
                                .stateRef(new EntityRef(s.id(), s.tempKey()))
                                .category(s.category)
                                .build())
                        .toList(),
                replaceTransitionRequests.stream()
                        .map(t -> TransitionDefinition.builder()
                                .transitionRef(new EntityRef(t.id(), t.tempKey()))
                                .sourceStateRef(t.source)
                                .targetStateRef(t.target)
                                .build())
                        .toList());
    }
}

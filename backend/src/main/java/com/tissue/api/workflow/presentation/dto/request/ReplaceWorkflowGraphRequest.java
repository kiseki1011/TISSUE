package com.tissue.api.workflow.presentation.dto.request;

import java.util.List;

import org.springframework.lang.Nullable;

import com.tissue.api.common.enums.ColorType;
import com.tissue.api.workflow.application.dto.ReplaceWorkflowGraphCommand;
import com.tissue.api.workflow.domain.service.EntityRef;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * TODO: 자세한 문서화 필요
 *  - 기존 status, transition은 id 전달
 *  - 새로 추가되는 status, transition은 tempKey 전달
 *  - tempKey는 클라이언트에서 생성. UUID 계열 권장(원한면 transliteration 사용)
 */
public record ReplaceWorkflowGraphRequest(
	@NotNull Long version,
	@NotEmpty List<ReplaceStatusRequest> replaceStatusRequests,
	@NotEmpty List<ReplaceTransitionRequest> replaceTransitionRequests
) {
	public record ReplaceStatusRequest(
		Long id,
		String tempKey,
		@Nullable @Size(max = 32) String label,
		@Nullable @Size(max = 255) String description,
		@NotNull ColorType color,
		@NotNull boolean initial,
		@NotNull boolean terminal
	) {
		public ReplaceStatusRequest {
			if ((id == null) == (tempKey == null)) {
				throw new IllegalArgumentException("Status must have exactly one of id or tempKey");
			}
		}
	}

	public record ReplaceTransitionRequest(
		Long id,
		String tempKey,
		@Nullable @Size(max = 32) String label,
		@Nullable @Size(max = 255) String description,
		@NotNull EntityRef source,
		@NotNull EntityRef target
	) {
		public ReplaceTransitionRequest {
			if ((id == null) == (tempKey == null)) {
				throw new IllegalArgumentException("Transition must have exactly one of id or tempKey");
			}
		}
	}

	public ReplaceWorkflowGraphCommand toCommand(String workspaceKey, Long workflowId) {
		return new ReplaceWorkflowGraphCommand(
			workspaceKey,
			workflowId,
			version,
			replaceStatusRequests.stream()
				.map(s -> new ReplaceWorkflowGraphCommand.StatusCommand(
					new EntityRef(s.id(), s.tempKey()),
					s.label(),
					s.description(),
					s.color(),
					s.initial(),
					s.terminal()
				))
				.toList(),
			replaceTransitionRequests.stream()
				.map(t -> new ReplaceWorkflowGraphCommand.TransitionCommand(
					new EntityRef(t.id(), t.tempKey()),
					t.label(),
					t.description(),
					t.source(),
					t.target()
				))
				.toList()
		);
	}
}

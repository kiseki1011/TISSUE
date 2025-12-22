package com.tissue.position.application.service.validator;

import org.springframework.stereotype.Component;

import com.tissue.common.vo.Label;
import com.tissue.position.application.port.out.PositionQueryRepository;
import com.tissue.position.domain.Position;
import com.tissue.workspace.domain.Workspace;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PositionValidator {

	private final PositionQueryRepository positionQueryRepository;

	public void ensureUniqueName(Workspace workspace, String name) {
		String normalizedName = Label.of(name).getNormalized();

		if (positionQueryRepository.existsByWorkspaceAndNameNormalized(workspace, normalizedName)) {
			throw new RuntimeException("Position name must be unique for workspace");
		}
	}

	public void ensureDeletable(Position position) {
		if (positionQueryRepository.existsByWorkspaceMembers(position)) {
			throw new RuntimeException(
				"There is a workspace member that is using this position. position id: %d, position name: %s"
					.formatted(position.getId(), position.getName())
			);
		}
	}
}

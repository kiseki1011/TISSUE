package com.tissue.position.validator;

import org.springframework.stereotype.Component;

import com.tissue.position.domain.model.Position;
import com.tissue.position.infrastructure.repository.PositionRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PositionValidator {

	private final PositionRepository positionRepository;

	public void ensureDeletable(Position position) {
		if (positionRepository.existsByWorkspaceMembers(position)) {
			// TODO: PositionCurrentlyUsedException, 더 좋은 이름이 있을까?
			//  - 메세지에는 사용 중이라 삭제 불가하다는 내용 필요
			throw new RuntimeException(
				"There is a workspace member that is using this position. position id: %d, position name: %s"
					.formatted(position.getId(), position.getName())
			);
		}
	}
}

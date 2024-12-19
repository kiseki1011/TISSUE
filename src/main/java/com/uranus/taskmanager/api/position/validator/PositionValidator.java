package com.uranus.taskmanager.api.position.validator;

import org.springframework.stereotype.Component;

import com.uranus.taskmanager.api.position.domain.Position;
import com.uranus.taskmanager.api.position.domain.repository.PositionRepository;
import com.uranus.taskmanager.api.position.exception.DuplicatePositionNameException;
import com.uranus.taskmanager.api.position.exception.PositionInUseException;

import lombok.RequiredArgsConstructor;

/**
 * Todo
 *  - 현재 validation 메서드의 이름은 "뭘 검증하나?"에 초점이 맞춰짐
 *  - 메서드 이름을 실제 검증하는 행위를 활용하는 것이 더 좋을지 고민이 됨
 *  - 예시: validateDuplicatePositionName -> validateCreatePosition (CreatePosition 안에는 중복 검증 존재)
 *  - 추후에 CreatePosition에 추가적인 검증이 필요하면, 해당하는 검증 메서드를 만들고 호출하면 됨
 */

@Component
@RequiredArgsConstructor
public class PositionValidator {

	private final PositionRepository positionRepository;

	public void validateDuplicatePositionName(String workspaceCode, String positionName) {
		if (positionRepository.existsByWorkspaceCodeAndName(workspaceCode, positionName)) {
			throw new DuplicatePositionNameException();
		}
	}

	public void validatePositionIsUsed(Position position) {
		if (positionRepository.existsByWorkspaceMembers(position)) {
			throw new PositionInUseException();
		}
	}
}

package com.uranus.taskmanager.fixture.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.common.ColorType;
import com.uranus.taskmanager.api.position.domain.Position;
import com.uranus.taskmanager.api.position.domain.repository.PositionRepository;
import com.uranus.taskmanager.api.workspace.domain.Workspace;

@Component
@Transactional
public class PositionRepositoryFixture {

	@Autowired
	private PositionRepository positionRepository;

	public Position createAndSavePosition(String name, Workspace workspace) {
		Position position = Position.builder()
			.name(name)
			.color(ColorType.BLACK)
			.description("This is a test position")
			.workspace(workspace)
			.build();
		return positionRepository.save(position);
	}
}

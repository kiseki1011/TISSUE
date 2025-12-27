package com.tissue.position.application.port.out;

import org.springframework.data.repository.Repository;

import com.tissue.position.domain.Position;

public interface PositionCommandRepository extends Repository<Position, Long> {

	Position save(Position position);

	void delete(Position position);
}

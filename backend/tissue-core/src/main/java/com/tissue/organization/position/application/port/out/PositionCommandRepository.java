package com.tissue.organization.position.application.port.out;

import com.tissue.organization.position.domain.Position;
import org.springframework.data.repository.Repository;

public interface PositionCommandRepository extends Repository<Position, Long> {

    Position save(Position position);

    void delete(Position position);
}

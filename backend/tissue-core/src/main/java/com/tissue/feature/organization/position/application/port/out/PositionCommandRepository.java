package com.tissue.feature.organization.position.application.port.out;

import com.tissue.feature.organization.position.domain.Position;
import org.springframework.data.repository.Repository;

public interface PositionCommandRepository extends Repository<Position, Long> {

    Position save(Position position);

    void delete(Position position);
}

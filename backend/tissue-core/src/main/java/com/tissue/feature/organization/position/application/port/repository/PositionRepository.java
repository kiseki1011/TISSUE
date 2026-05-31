package com.tissue.feature.organization.position.application.port.repository;

import com.tissue.feature.organization.position.domain.Position;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface PositionRepository extends Repository<Position, Long> {

    Position save(Position position);

    void delete(Position position);

    Optional<Position> findById(Long id);

    boolean existsByName_NormalizedName(String normalizedName);

    @Query("""
           SELECT p
           FROM Position p
           ORDER BY p.id ASC
       """)
    List<Position> findAllOrderById();
}

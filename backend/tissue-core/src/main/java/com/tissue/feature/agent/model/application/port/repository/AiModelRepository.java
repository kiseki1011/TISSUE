package com.tissue.feature.agent.model.application.port.repository;

import com.tissue.feature.agent.model.domain.AiModel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface AiModelRepository extends Repository<AiModel, Long> {

    AiModel save(AiModel model);

    void delete(AiModel model);

    Optional<AiModel> findById(Long id);

    boolean existsByName_NormalizedName(String normalizedName);

    @Query("""
           SELECT m
           FROM AiModel m
           ORDER BY m.id ASC
       """)
    List<AiModel> findAllOrderById();
}

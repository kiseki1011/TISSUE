package com.tissue.feature.workflow.application.port.repository;

import com.tissue.feature.workflow.domain.Workflow;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface WorkflowRepository extends Repository<Workflow, Long> {

    Workflow save(Workflow workflow);

    void delete(Workflow workflow);

    Optional<Workflow> findById(Long id);

    @Query("SELECT w FROM Workflow w ORDER BY w.name.displayName ASC")
    List<Workflow> findAllByOrderByName();

    boolean existsByName_NormalizedName(String name);
}

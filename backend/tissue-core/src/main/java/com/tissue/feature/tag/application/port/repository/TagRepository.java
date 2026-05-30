package com.tissue.feature.tag.application.port.repository;

import com.tissue.feature.project.domain.Project;
import com.tissue.feature.tag.domain.Tag;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.Repository;

public interface TagRepository extends Repository<Tag, Long> {

    Tag save(Tag tag);

    void delete(Tag tag);

    @EntityGraph(attributePaths = {"project"})
    Optional<Tag> findByProjectKeyAndId(String projectKey, Long id);

    @EntityGraph(attributePaths = {"project"})
    Optional<Tag> findById(Long id);

    boolean existsByName_NormalizedNameAndProject(String normalizedName, Project project);

    Page<Tag> findAllByProjectKey(String projectKey, Pageable pageable);
}

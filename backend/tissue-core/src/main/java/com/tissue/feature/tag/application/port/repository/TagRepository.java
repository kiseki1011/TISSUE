package com.tissue.feature.tag.application.port.repository;

import com.tissue.feature.project.domain.Project;
import com.tissue.feature.tag.domain.Tag;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.Repository;

public interface TagRepository extends Repository<Tag, Long> {

    Tag save(Tag tag);

    void delete(Tag tag);

    @EntityGraph(attributePaths = {"project"})
    Optional<Tag> findByWorkspaceKeyAndProjectKeyAndId(String workspaceKey, String projectKey, Long id);

    @EntityGraph(attributePaths = {"project"})
    Optional<Tag> findByWorkspaceKeyAndId(String workspaceKey, Long id);

    boolean existsByName_NormalizedNameAndProject(String normalizedName, Project project);

    List<Tag> findAllByWorkspaceKeyAndProjectKey(String workspaceKey, String projectKey);
}

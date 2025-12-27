package com.tissue.issuetype.application.port.out;

import com.tissue.issuetype.domain.IssueType;
import com.tissue.project.domain.Project;
import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface IssueTypeQueryRepository extends Repository<IssueType, Long> {

    Optional<IssueType> findByIdAndProjectKey(Long id, String projectKey);

    Optional<IssueType> findByIdAndProject(Long id, Project project);

    boolean existsByName_NormalizedAndProject(String label, Project project);
}

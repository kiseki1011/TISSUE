package com.tissue.feature.project.application.port.repository;

import com.tissue.feature.project.domain.Project;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface ProjectQueryRepository extends Repository<Project, Long> {

    Optional<Project> findById(Long id);

    @Query("SELECT p FROM Project p WHERE p.key.value = :projectKey")
    Optional<Project> findByKey(@Param("projectKey") String projectKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Project p WHERE p.key.value = :projectKey")
    Optional<Project> findByProjectKeyWithLock(@Param("projectKey") String projectKey);

    @Query(value = """
            SELECT p.*
            FROM project p
            WHERE p.project_key = :projectKey
              AND p.soft_deleted = true
            """, nativeQuery = true)
    Optional<Project> findDeletedByKey(@Param("projectKey") String projectKey);

    @Query("SELECT COUNT(p) > 0 FROM Project p WHERE p.key.value = :projectKey")
    boolean existsByKey(@Param("projectKey") String projectKey);

    @Query(value = "SELECT EXISTS(SELECT 1 FROM project p WHERE p.project_key = :projectKey)", nativeQuery = true)
    boolean existsByKeyIncludingSoftDeleted(@Param("projectKey") String projectKey);

    @Query(value = """
            SELECT p FROM Project p
            WHERE (:includeArchived = true OR p.archived = false)
            """, countQuery = """
            SELECT COUNT(p) FROM Project p
            WHERE (:includeArchived = true OR p.archived = false)
            """)
    Page<Project> findAllProjects(@Param("includeArchived") boolean includeArchived, Pageable pageable);

    @Query(value = """
            SELECT p FROM Project p
            WHERE (:includeArchived = true OR p.archived = false)
              AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(p.key.value) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """, countQuery = """
            SELECT COUNT(p) FROM Project p
            WHERE (:includeArchived = true OR p.archived = false)
              AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(p.key.value) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Project> findAllByKeyword(
            @Param("includeArchived") boolean includeArchived, @Param("keyword") String keyword, Pageable pageable);
}

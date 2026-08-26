package com.tissue.feature.vcs.application.port.repository;

import com.tissue.feature.vcs.domain.ProjectVcsIntegration;
import com.tissue.feature.vcs.domain.enums.VcsProvider;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface ProjectVcsIntegrationRepository extends Repository<ProjectVcsIntegration, Long> {

    ProjectVcsIntegration save(ProjectVcsIntegration vcsIntegration);

    void delete(ProjectVcsIntegration vcsIntegration);

    Optional<ProjectVcsIntegration> findByProjectKeyAndProvider(String projectKey, VcsProvider provider);

    /**
     * Reads the secret column raw, bypassing decryption, so rows written before encryption existed can be
     * found. The pattern is bound rather than inlined because the prefix itself contains a colon, which a
     * query would otherwise read as a named parameter.
     */
    @Query(
            value = "SELECT id, webhook_secret FROM project_vcs_integration WHERE webhook_secret "
                    + "NOT LIKE :encryptedPattern",
            nativeQuery = true)
    List<Object[]> findRowsWithPlaintextSecret(@Param("encryptedPattern") String encryptedPattern);

    @Modifying
    @Query(value = "UPDATE project_vcs_integration SET webhook_secret = :secret WHERE id = :id", nativeQuery = true)
    void applyEncryptedSecret(@Param("id") Long id, @Param("secret") String secret);
}

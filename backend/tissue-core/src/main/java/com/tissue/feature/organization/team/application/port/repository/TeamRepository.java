package com.tissue.feature.organization.team.application.port.repository;

import com.tissue.feature.organization.team.domain.Team;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface TeamRepository extends Repository<Team, Long> {

    Team save(Team team);

    void delete(Team team);

    Optional<Team> findById(Long id);

    boolean existsByName_NormalizedName(String normalizedName);

    @Query("""
           SELECT t
           FROM Team t
           ORDER BY t.id ASC
       """)
    List<Team> findAllOrderById();
}

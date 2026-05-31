package com.tissue.feature.member.application.port.repository;

import com.tissue.feature.member.domain.Member;
import com.tissue.feature.organization.position.domain.Position;
import com.tissue.feature.organization.team.domain.Team;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface MemberCommandRepository extends Repository<Member, Long> {

    Member save(Member member);

    @Modifying
    @Query("UPDATE Member m SET m.position = null WHERE m.position = :position")
    void clearPositionAssignments(Position position);

    @Modifying
    @Query("UPDATE Member m SET m.team = null WHERE m.team = :team")
    void clearTeamAssignments(Team team);
}

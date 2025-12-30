package com.tissue.workspace.application.port.out;

import com.tissue.member.domain.Member;
import com.tissue.workspace.domain.Invitation;
import com.tissue.workspace.domain.enums.InvitationStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface InvitationQueryRepository extends Repository<Invitation, Long> {

    Optional<Invitation> findById(Long id);

    Optional<Invitation> findByIdAndMember(Long id, Member member);

    @Query("""
                SELECT i
                FROM Invitation i
                JOIN FETCH i.workspace
                WHERE i.member.id = :memberId
                  AND i.status = :status
            """)
    List<Invitation> findAllByMemberIdAndStatus(
            @Param("memberId") Long memberId, @Param("status") InvitationStatus status);

    @Query("SELECT i.member.id FROM Invitation i "
            + "WHERE i.workspaceKey = :workspaceKey "
            + "AND i.member.id IN :candidateIds "
            + "AND i.status = 'PENDING'")
    Set<Long> findPendingMemberIds(
            @Param("workspaceKey") String workspaceKey, @Param("candidateIds") Collection<Long> candidateIds);
}

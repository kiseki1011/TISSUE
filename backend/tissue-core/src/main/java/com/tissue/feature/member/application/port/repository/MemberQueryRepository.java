package com.tissue.feature.member.application.port.repository;

import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.MemberStatus;
import com.tissue.feature.member.domain.SystemRole;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface MemberQueryRepository extends Repository<Member, Long>, JpaSpecificationExecutor<Member> {

    Optional<Member> findById(Long id);

    Optional<Member> findByIdAndStatus(Long id, MemberStatus status);

    Optional<Member> findByEmailAndStatus(String email, MemberStatus status);

    /**
     * Members for anonymization. In the given status (typically
     * {@code DELETED}) and withdrawn before cutoff (= now - retention).
     */
    List<Member> findAllByStatusAndDeletedAtBefore(MemberStatus status, Instant cutoff);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    long count();

    long countByStatus(MemberStatus status);

    long countByRoleAndStatus(SystemRole role, MemberStatus status);

    List<Member> findAllByIdInAndStatus(Set<Long> ids, MemberStatus status);

    List<Member> findAllByOwner_IdAndStatus(Long ownerId, MemberStatus status);

    Optional<Member> findByIdAndOwner_Id(Long id, Long ownerId);

    boolean existsByOwner_IdAndNameAndStatus(Long ownerId, String name, MemberStatus status);

    @Query("SELECT m.id as memberId, m.email as email, m.language as language " + "FROM Member m WHERE m.id = :id")
    Optional<MemberContactInfo> findContactById(@Param("id") Long id);

    @Query("SELECT m.id as memberId, m.email as email, m.language as language " + "FROM Member m WHERE m.id IN :ids")
    List<MemberContactInfo> findAllContactsByIdIn(@Param("ids") Set<Long> ids);

    @Query("SELECT m.id as memberId, m.email as email, m.language as language "
            + "FROM Member m WHERE m.username IN :usernames")
    List<MemberContactInfo> findAllContactsByUsernameIn(@Param("usernames") Set<String> usernames);
}

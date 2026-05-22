package com.tissue.feature.member.application.port.repository;

import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.MemberStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.repository.Repository;

public interface MemberQueryRepository extends Repository<Member, Long> {

    Optional<Member> findById(Long id);

    Optional<Member> findByEmail(String email);

    Optional<Member> findByIdAndStatus(Long id, MemberStatus status);

    Optional<Member> findByEmailAndStatus(String email, MemberStatus status);

    List<Member> findAllByEmailInAndStatus(Set<String> emails, MemberStatus status);

    /**
     * Members for anonymization. In the given status (typically
     * {@code DELETED}) and withdrawn before cutoff (= now - retention).
     */
    List<Member> findAllByStatusAndDeletedAtBefore(MemberStatus status, Instant cutoff);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    long count();
}

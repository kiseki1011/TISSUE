package com.tissue.feature.member.application.port.repository;

import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.MemberStatus;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.repository.Repository;

public interface MemberQueryRepository extends Repository<Member, Long> {

    Optional<Member> findById(Long id);

    Optional<Member> findByEmail(String email);

    Optional<Member> findByIdAndStatus(Long id, MemberStatus status);

    Optional<Member> findByEmailAndStatus(String email, MemberStatus status);

    Optional<Member> findByUsernameAndStatus(String username, MemberStatus status);

    List<Member> findAllByEmailInAndStatus(Set<String> emails, MemberStatus status);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}

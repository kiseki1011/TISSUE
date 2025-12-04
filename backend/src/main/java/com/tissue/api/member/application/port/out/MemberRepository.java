package com.tissue.api.member.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.member.domain.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {

	Optional<Member> findByEmail(String email);

	List<Member> findAllByEmailIn(Set<String> emails);

	boolean existsByEmail(String email);

	boolean existsByUsername(String username);
}

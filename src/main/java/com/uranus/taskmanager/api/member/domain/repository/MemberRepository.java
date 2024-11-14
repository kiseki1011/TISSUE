package com.uranus.taskmanager.api.member.domain.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.uranus.taskmanager.api.member.domain.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {
	Optional<Member> findByLoginId(String username);

	Optional<Member> findByEmail(String email);

	Optional<Member> findByLoginIdOrEmail(String email, String loginId);

	boolean existsByLoginId(String loginId);

	boolean existsByEmail(String email);
}

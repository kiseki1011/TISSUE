package com.uranus.taskmanager.api.member.domain.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.uranus.taskmanager.api.member.domain.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {
	Optional<Member> findByLoginId(String username);

	Optional<Member> findByLoginIdOrEmail(String email, String loginId);

	@Query("SELECT m FROM Member m "
		+ "WHERE (m.loginId = :identifier OR m.email = :identifier)")
	Optional<Member> findByMemberIdentifier(
		@Param("identifier") String identifier
	);

	boolean existsByLoginId(String loginId);

	boolean existsByEmail(String email);
}

package com.uranus.taskmanager.api.member.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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

	@Query("SELECT m FROM Member m "
		+ "WHERE m.email IN :identifiers OR m.loginId IN :identifiers")
	List<Member> findAllByEmailInOrLoginIdIn(@Param("identifiers") Set<String> identifiers);

	boolean existsByLoginId(String loginId);

	boolean existsByEmail(String email);
}

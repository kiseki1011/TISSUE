package com.tissue.api.member.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.member.domain.model.Member;

public interface MemberQueryRepository extends JpaRepository<Member, Long> {
}

package com.tissue.api.member.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.member.domain.Member;

public interface MemberQueryRepository extends JpaRepository<Member, Long> {
}

package com.tissue.member.application.port.out;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.member.domain.Member;

public interface MemberQueryRepository extends JpaRepository<Member, Long> {
}

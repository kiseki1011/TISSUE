package com.tissue.feature.member.application.port.repository;

import com.tissue.feature.member.domain.Member;
import org.springframework.data.repository.Repository;

public interface MemberCommandRepository extends Repository<Member, Long> {

    Member save(Member member);
}

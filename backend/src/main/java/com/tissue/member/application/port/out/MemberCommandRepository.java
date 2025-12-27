package com.tissue.member.application.port.out;

import java.util.List;

import org.springframework.data.repository.Repository;

import com.tissue.member.domain.Member;

public interface MemberCommandRepository extends Repository<Member, Long> {

	Member save(Member member);

	List<Member> saveAll(Iterable<Member> members);
}

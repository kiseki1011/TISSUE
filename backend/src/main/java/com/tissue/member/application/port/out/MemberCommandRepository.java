package com.tissue.member.application.port.out;

import com.tissue.member.domain.Member;
import java.util.List;
import org.springframework.data.repository.Repository;

public interface MemberCommandRepository extends Repository<Member, Long> {

    Member save(Member member);

    List<Member> saveAll(Iterable<Member> members);
}

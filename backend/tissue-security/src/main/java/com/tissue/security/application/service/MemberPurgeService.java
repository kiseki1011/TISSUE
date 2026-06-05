package com.tissue.security.application.service;

import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.security.adapter.persistence.PersonalAccessTokenRepository;
import com.tissue.security.application.port.repository.AuthenticationIdentityRepository;
import com.tissue.security.application.port.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberPurgeService {

    private final AuthenticationIdentityRepository authenticationIdentityRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PersonalAccessTokenRepository personalAccessTokenRepository;
    private final MemberCommandRepository memberCommandRepository;

    @Transactional
    public void purge(Member member) {
        Long memberId = member.getId();
        authenticationIdentityRepository.deleteByMemberId(memberId);
        refreshTokenRepository.deleteByMemberId(memberId);
        personalAccessTokenRepository.deleteAllByMember_Id(memberId);
        member.anonymize();
        memberCommandRepository.save(member);
    }
}

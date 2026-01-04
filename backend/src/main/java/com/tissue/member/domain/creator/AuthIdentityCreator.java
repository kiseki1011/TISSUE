package com.tissue.member.domain.creator;

import com.tissue.member.domain.AuthIdentity;
import com.tissue.member.domain.AuthProvider;
import com.tissue.member.domain.Member;

public interface AuthIdentityCreator {

    boolean supports(AuthProvider provider);

    /**
     * 인증 수단을 생성
     *
     * @param member 연결할 회원
     * @param identifier 식별자 (이메일 등)
     * @param credential 비밀번호 (OAuth의 경우 null 가능)
     * @return 생성된 AuthIdentity
     */
    AuthIdentity create(Member member, String identifier, String credential);
}

package com.tissue.domain.creator;

import com.tissue.domain.AuthenticationIdentity;
import com.tissue.domain.AuthenticationProvider;
import com.tissue.feature.member.domain.Member;
import org.jspecify.annotations.Nullable;

// TODO: Javadoc
public interface AuthenticationIdentityCreator {

    boolean supports(AuthenticationProvider provider);

    /**
     * 인증 수단을 생성
     *
     * @param member 연결할 회원
     * @param identifier 식별자 (이메일 등)
     * @param credential 비밀번호 (OAuth의 경우 null 가능)
     * @return 생성된 AuthIdentity
     */
    AuthenticationIdentity create(Member member, String identifier, @Nullable String credential);
}

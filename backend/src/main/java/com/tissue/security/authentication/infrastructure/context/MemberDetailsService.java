package com.tissue.security.authentication.infrastructure.context;

import com.tissue.member.application.port.out.AuthIdentityRepository;
import com.tissue.member.domain.AuthIdentity;
import com.tissue.member.domain.AuthProvider;
import com.tissue.member.domain.Member;
import com.tissue.member.domain.MemberStatus;
import com.tissue.security.authentication.domain.MemberDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security의 인증 과정에서 사용자 정보를 로드하는 서비스
 *
 * <p>로그인 시 이메일을 입력받아, 해당 이메일로 연결된 `EMAIL` 타입의 `AuthIdentity`를 조회<br>
 * 조회된 Identity에서 `Member` 정보와 `Credential`(비밀번호)을 추출하여 `MemberUserDetails`를 구성
 * </p>
 */
@Service
@RequiredArgsConstructor
public class MemberDetailsService implements UserDetailsService {

    private final AuthIdentityRepository authIdentityRepository;

    /**
     * 이메일(Username)을 기반으로 사용자 인증 정보를 조회.
     *
     * <p>이 메서드는 주로 `AuthenticationManager`가 비밀번호 검증을 수행하기 위해 호출
     * 따라서 반드시 비밀번호가 포함된 `MemberUserDetails`를 반환해야 함</p>
     *
     * @param email 로그인 이메일
     * @return UserDetails 객체 (Member 정보 + 비밀번호)
     * @throws UsernameNotFoundException 해당 이메일의 인증 정보가 없거나 회원이 비활성 상태인 경우
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        AuthIdentity authIdentity = authIdentityRepository
                .findByProviderAndIdentifier(AuthProvider.EMAIL, email)
                .orElseThrow(() -> new UsernameNotFoundException("Member not found for email: " + email));

        Member member = authIdentity.getMember();

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new UsernameNotFoundException("Member is not active: " + email);
        }

        return new MemberDetails(member, authIdentity.getCredential());
    }
}

package com.tissue.security.authentication.application.port.out;

// TODO: 사용하지 말고 그냥 컨트롤러에서 memberId를 넘기자
public interface CurrentMemberProvider {
    Long getCurrentMemberId();
}

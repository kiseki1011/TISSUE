package com.tissue.member.application.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.member.application.service.MemberProfileService;
import com.tissue.feature.member.domain.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberCommandServiceTest {

    @Mock
    MemberFinder memberFinder;

    @InjectMocks
    MemberProfileService sut;

    @Nested
    @DisplayName("update name")
    class UpdateName {
        @Test
        @DisplayName("success: updates name")
        void success_UpdateName() {
            Long memberId = 1L;
            String newName = "newName";

            Member member = mock(Member.class);
            given(memberFinder.getActiveBy(memberId)).willReturn(member);

            sut.updateName(newName, memberId);

            then(member).should().updateName(newName);
        }
    }
}

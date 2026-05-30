package com.tissue.feature.project.application.service.finder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.application.port.repository.ProjectMemberQueryRepository;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.project.domain.exception.ProjectMemberNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectAccessResolverTest {

    @Mock
    private ProjectMemberQueryRepository queryRepository;

    @Mock
    private ProjectFinder projectFinder;

    @Mock
    private MemberFinder memberFinder;

    @InjectMocks
    private ProjectAccessResolver sut;

    @Test
    @DisplayName("returns the real ProjectMember when the actor is a member")
    void returnsRealMember() {
        // given
        ProjectMember member = mock(ProjectMember.class);
        given(queryRepository.findWithMemberByProjectKeyAndMemberId("PROJ", 1L)).willReturn(Optional.of(member));

        // when / then
        assertThat(sut.resolveByProjectKey("PROJ", 1L)).isSameAs(member);
    }

    @Test
    @DisplayName("returns a synthetic override membership when a non-member actor is a system admin")
    void returnsOverrideForNonMemberSystemAdmin() {
        // given
        given(queryRepository.findWithMemberByProjectKeyAndMemberId("PROJ", 1L)).willReturn(Optional.empty());
        Member admin = Member.createAsAdmin("admin@tissue.com", "admin", "Admin");
        given(memberFinder.getActiveById(1L)).willReturn(admin);
        Project project = mock(Project.class);
        given(project.getKey()).willReturn("PROJ");
        given(projectFinder.getByProjectKey("PROJ")).willReturn(project);

        // when
        ProjectMember result = sut.resolveByProjectKey("PROJ", 1L);

        // then
        assertThat(result.getMember()).isSameAs(admin);
        assertThat(result.getProject()).isSameAs(project);
        assertThat(result.isManager()).isFalse();
    }

    @Test
    @DisplayName("throws ProjectMemberNotFoundException when a non-member actor is not a system admin")
    void throwsForNonMemberNonAdmin() {
        // given
        given(queryRepository.findWithMemberByProjectKeyAndMemberId("PROJ", 1L)).willReturn(Optional.empty());
        Member user = Member.create("user@tissue.com", "user", "User");
        given(memberFinder.getActiveById(1L)).willReturn(user);

        // when / then
        assertThatThrownBy(() -> sut.resolveByProjectKey("PROJ", 1L))
                .isInstanceOf(ProjectMemberNotFoundException.class);
    }
}

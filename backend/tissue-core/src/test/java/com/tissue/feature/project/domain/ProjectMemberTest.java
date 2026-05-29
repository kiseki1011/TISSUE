package com.tissue.feature.project.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.tissue.feature.member.domain.Member;
import com.tissue.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ProjectMemberTest {

    private Project project() {
        return TestFixtures.project("PROJ");
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("creates a MEMBER-role project member that delegates identity to the member")
        void createsMember() {
            // given
            Member member = TestFixtures.member("alice");

            // when
            ProjectMember pm = ProjectMember.create(project(), member);

            // then
            assertThat(pm.getRole()).isEqualTo(ProjectRole.MEMBER);
            assertThat(pm.isManager()).isFalse();
            assertThat(pm.getMember()).isEqualTo(member);
            assertThat(pm.getDisplayName()).isEqualTo("alice");
        }
    }

    @Nested
    @DisplayName("createManager()")
    class CreateManager {

        @Test
        @DisplayName("creates a MANAGER-role project member")
        void createsManager() {
            // given
            Member member = TestFixtures.member("boss");

            // when
            ProjectMember pm = ProjectMember.createManager(project(), member);

            // then
            assertThat(pm.getRole()).isEqualTo(ProjectRole.MANAGER);
            assertThat(pm.isManager()).isTrue();
            assertThat(pm.getMember()).isEqualTo(member);
        }
    }
}

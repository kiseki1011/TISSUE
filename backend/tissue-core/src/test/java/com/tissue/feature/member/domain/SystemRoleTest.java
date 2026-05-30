package com.tissue.feature.member.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SystemRoleTest {

    @Nested
    @DisplayName("getAuthority()")
    class GetAuthority {

        @Test
        @DisplayName("prefixes the role name with ROLE_")
        void prefixesRoleName() {
            assertThat(SystemRole.SUPER_ADMIN.getAuthority()).isEqualTo("ROLE_SUPER_ADMIN");
            assertThat(SystemRole.ADMIN.getAuthority()).isEqualTo("ROLE_ADMIN");
            assertThat(SystemRole.USER.getAuthority()).isEqualTo("ROLE_USER");
        }
    }

    @Nested
    @DisplayName("isEqualOrHigherThan()")
    class IsEqualOrHigherThan {

        @Test
        @DisplayName("SUPER_ADMIN outranks ADMIN and USER, and equals itself")
        void superAdminOutranksAll() {
            assertThat(SystemRole.SUPER_ADMIN.isEqualOrHigherThan(SystemRole.SUPER_ADMIN))
                    .isTrue();
            assertThat(SystemRole.SUPER_ADMIN.isEqualOrHigherThan(SystemRole.ADMIN))
                    .isTrue();
            assertThat(SystemRole.SUPER_ADMIN.isEqualOrHigherThan(SystemRole.USER))
                    .isTrue();
        }

        @Test
        @DisplayName("ADMIN outranks USER but not SUPER_ADMIN")
        void adminRanking() {
            assertThat(SystemRole.ADMIN.isEqualOrHigherThan(SystemRole.USER)).isTrue();
            assertThat(SystemRole.ADMIN.isEqualOrHigherThan(SystemRole.ADMIN)).isTrue();
            assertThat(SystemRole.ADMIN.isEqualOrHigherThan(SystemRole.SUPER_ADMIN))
                    .isFalse();
        }

        @Test
        @DisplayName("USER does not outrank ADMIN or SUPER_ADMIN")
        void userDoesNotOutrank() {
            assertThat(SystemRole.USER.isEqualOrHigherThan(SystemRole.USER)).isTrue();
            assertThat(SystemRole.USER.isEqualOrHigherThan(SystemRole.ADMIN)).isFalse();
            assertThat(SystemRole.USER.isEqualOrHigherThan(SystemRole.SUPER_ADMIN))
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("isHigherThan()")
    class IsHigherThan {

        @Test
        @DisplayName("is strict: an equal role is not higher")
        void strictComparison() {
            assertThat(SystemRole.ADMIN.isHigherThan(SystemRole.ADMIN)).isFalse();
            assertThat(SystemRole.SUPER_ADMIN.isHigherThan(SystemRole.ADMIN)).isTrue();
            assertThat(SystemRole.USER.isHigherThan(SystemRole.ADMIN)).isFalse();
        }
    }
}

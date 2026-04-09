package com.tissue.feature.wiki.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.wiki.domain.enums.SemanticUpdateType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SnapshotVersionTest {

    @Nested
    @DisplayName("initial snapshot version")
    class InitialSnapshotVersion {

        @Test
        @DisplayName("success: initial version should be 1.0.0")
        void successInitialVersion() {
            // when
            SnapshotVersion version = SnapshotVersion.initial();

            // then
            assertThat(version.getMajor()).isEqualTo(1);
            assertThat(version.getMinor()).isZero();
            assertThat(version.getPatch()).isZero();
            assertThat(version.toString()).isEqualTo("1.0.0");
        }
    }

    @Nested
    @DisplayName("bump snapshot version")
    class BumpSnapshotVersion {

        @Test
        @DisplayName("success: MAJOR version bump increases major and resets minor and patch")
        void successMajorBump() {
            // given
            SnapshotVersion version = new SnapshotVersion(1, 2, 3);

            // when
            SnapshotVersion bumped = version.bumpVersion(SemanticUpdateType.MAJOR);

            // then
            assertThat(bumped).isEqualTo(new SnapshotVersion(2, 0, 0));
        }

        @Test
        @DisplayName("success: MINOR bump increases minor and resets patch")
        void successMinorBump() {
            // given
            SnapshotVersion version = new SnapshotVersion(1, 2, 3);

            // when
            SnapshotVersion bumped = version.bumpVersion(SemanticUpdateType.MINOR);

            // then
            assertThat(bumped).isEqualTo(new SnapshotVersion(1, 3, 0));
        }

        @Test
        @DisplayName("success: PATCH bump increases patch")
        void successPatchBump() {
            // given
            SnapshotVersion version = new SnapshotVersion(1, 2, 3);

            // when
            SnapshotVersion bumped = version.bumpVersion(SemanticUpdateType.PATCH);

            // then
            assertThat(bumped).isEqualTo(new SnapshotVersion(1, 2, 4));
        }
    }

    @Nested
    @DisplayName("snapshot version validation")
    class SnapshotVersionValidation {

        @Test
        @DisplayName("fail: negative major throws IllegalArgumentException")
        void failNegativeMajor() {
            // when & then
            assertThatThrownBy(() -> new SnapshotVersion(-1, 0, 0)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("compare snapshot version")
    class SnapshotVersionCompareTo {

        @Test
        @DisplayName("success: higher major is greater")
        void successHigherMajorIsGreater() {
            assertThat(new SnapshotVersion(2, 0, 0)).isGreaterThan(new SnapshotVersion(1, 9, 9));
        }

        @Test
        @DisplayName("success: higher minor is greater when major is equal")
        void successHigherMinorIsGreater() {
            assertThat(new SnapshotVersion(1, 2, 0)).isGreaterThan(new SnapshotVersion(1, 1, 9));
        }

        @Test
        @DisplayName("success: higher patch is greater when major and minor are equal")
        void successHigherPatchIsGreater() {
            assertThat(new SnapshotVersion(1, 1, 2)).isGreaterThan(new SnapshotVersion(1, 1, 1));
        }

        @Test
        @DisplayName("success: equal versions compare to zero")
        void successEqualVersions() {
            assertThat(new SnapshotVersion(1, 2, 3)).isEqualByComparingTo(new SnapshotVersion(1, 2, 3));
        }
    }

    @Nested
    @DisplayName("equals and hashCode")
    class SnapshotVersionEqualsAndHashCode {

        @Test
        @DisplayName("success: same version numbers are equal")
        void successSameVersionsAreEqual() {
            assertThat(new SnapshotVersion(1, 2, 3)).isEqualTo(new SnapshotVersion(1, 2, 3));
        }

        @Test
        @DisplayName("success: different version numbers are not equal")
        void successDifferentVersionsAreNotEqual() {
            assertThat(new SnapshotVersion(1, 2, 3)).isNotEqualTo(new SnapshotVersion(1, 2, 4));
        }
    }
}

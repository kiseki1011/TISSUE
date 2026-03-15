package com.tissue.feature.issue.domain;

import static com.tissue.feature.issue.domain.exception.IssueErrorCode.DUE_DATE_MUST_BE_FUTURE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.shared.exception.base.BadRequestException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class IssueScheduleTest {

    @Nested
    @DisplayName("create schedule")
    class CreateSchedule {

        @Test
        @DisplayName("success: create with null dueAt")
        void successCreateWithNullDueAt() {
            // given & when
            IssueSchedule schedule = IssueSchedule.of(null);

            // then
            assertThat(schedule.getDueAt()).isNull();
        }

        @Test
        @DisplayName("success: create with future dueAt")
        void successCreateWithFutureDueAt() {
            // given
            Instant future = Instant.now().plus(7, ChronoUnit.DAYS);

            // when
            IssueSchedule schedule = IssueSchedule.of(future);

            // then
            assertThat(schedule.getDueAt()).isEqualTo(future);
        }

        @Test
        @DisplayName("fail: throws BadRequestException if dueAt is in the past")
        void failCreateWithPastDueAt() {
            // given
            Instant past = Instant.now().minus(1, ChronoUnit.DAYS);

            // when & then
            assertThatThrownBy(() -> IssueSchedule.of(past))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("errorCode")
                    .isEqualTo(DUE_DATE_MUST_BE_FUTURE);
        }
    }

    @Nested
    @DisplayName("lifecycle timestamps")
    class LifecycleTimestamps {

        @Test
        @DisplayName("success: markStarted sets startedAt")
        void successMarkStarted() {
            // given
            IssueSchedule schedule = IssueSchedule.of(null);

            // when
            schedule.markStarted();

            // then
            assertThat(schedule.getStartedAt()).isNotNull();
        }

        @Test
        @DisplayName("success: markStarted is idempotent")
        void successMarkStartedIdempotent() {
            // given
            IssueSchedule schedule = IssueSchedule.of(null);
            schedule.markStarted();
            Instant firstStartedAt = schedule.getStartedAt();

            // when
            schedule.markStarted();

            // then
            assertThat(schedule.getStartedAt()).isEqualTo(firstStartedAt);
        }

        @Test
        @DisplayName("success: markResolved sets resolvedAt")
        void successMarkResolved() {
            // given
            IssueSchedule schedule = IssueSchedule.of(null);

            // when
            schedule.markResolved();

            // then
            assertThat(schedule.getResolvedAt()).isNotNull();
        }

        @Test
        @DisplayName("success: markResolved is idempotent")
        void successMarkResolvedIdempotent() {
            // given
            IssueSchedule schedule = IssueSchedule.of(null);
            schedule.markResolved();
            Instant firstResolvedAt = schedule.getResolvedAt();

            // when
            schedule.markResolved();

            // then
            assertThat(schedule.getResolvedAt()).isEqualTo(firstResolvedAt);
        }

        @Test
        @DisplayName("success: clearResolved nullifies resolvedAt")
        void successClearResolved() {
            // given
            IssueSchedule schedule = IssueSchedule.of(null);
            schedule.markResolved();
            assertThat(schedule.getResolvedAt()).isNotNull();

            // when
            schedule.clearResolved();

            // then
            assertThat(schedule.getResolvedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("update due date")
    class UpdateDueDate {

        @Test
        @DisplayName("fail: throws BadRequestException if new dueAt is in the past")
        void failUpdateWithPastDueAt() {
            // given
            IssueSchedule schedule = IssueSchedule.of(null);
            Instant past = Instant.now().minus(1, ChronoUnit.DAYS);

            // when & then
            assertThatThrownBy(() -> schedule.updateDueDate(past))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("errorCode")
                    .isEqualTo(DUE_DATE_MUST_BE_FUTURE);
        }

        @Test
        @DisplayName("success: update dueAt to null clears it")
        void successUpdateDueAtToNull() {
            // given
            Instant future = Instant.now().plus(7, ChronoUnit.DAYS);
            IssueSchedule schedule = IssueSchedule.of(future);

            // when
            schedule.updateDueDate(null);

            // then
            assertThat(schedule.getDueAt()).isNull();
        }
    }
}

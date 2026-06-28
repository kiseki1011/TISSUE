package com.tissue.feature.organization.position.application.service;

import static com.tissue.feature.organization.position.domain.exception.PositionErrorCode.DUPLICATE_POSITION_NAME;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.organization.position.application.dto.request.CreatePositionCommand;
import com.tissue.feature.organization.position.application.dto.request.PatchPositionCommand;
import com.tissue.feature.organization.position.application.port.repository.PositionRepository;
import com.tissue.feature.organization.position.application.service.finder.PositionFinder;
import com.tissue.feature.organization.position.application.service.validator.PositionValidator;
import com.tissue.feature.organization.position.domain.Position;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.exception.base.ResourceConflictException;
import com.tissue.shared.vo.Name;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.jackson.nullable.JsonNullable;

/**
 * System-admin authorization is enforced at the web layer via {@code @RequireSystemAdmin}
 * (Spring method security), not in this service — so these tests cover only behavior.
 */
@ExtendWith(MockitoExtension.class)
class PositionServiceTest {

    @Mock
    private PositionFinder positionFinder;

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private PositionValidator positionValidator;

    @Mock
    private MemberCommandRepository memberCommandRepository;

    @InjectMocks
    private PositionService sut;

    @Nested
    @DisplayName("create position")
    class CreatePosition {

        @Test
        @DisplayName("success: validates uniqueness and saves the position")
        void successCreatePosition() {
            // given
            Name name = Name.of("Backend Engineer");
            Position position = mock(Position.class);

            CreatePositionCommand cmd = CreatePositionCommand.builder()
                    .name(name)
                    .description("backend")
                    .color(ColorType.ANSI_BLUE)
                    .build();

            given(positionRepository.save(any(Position.class))).willReturn(position);

            // when
            sut.create(cmd, 1L);

            // then
            then(positionValidator).should().ensureUniqueLabel(name);
            then(positionRepository).should().save(any(Position.class));
        }

        @Test
        @DisplayName("fail: throws ResourceConflictException if position name is duplicate")
        void failCreate_If_DuplicateName() {
            // given
            Name name = Name.of("Backend Engineer");

            CreatePositionCommand cmd = CreatePositionCommand.builder()
                    .name(name)
                    .description(null)
                    .color(ColorType.ANSI_BLUE)
                    .build();

            willThrow(new ResourceConflictException(DUPLICATE_POSITION_NAME))
                    .given(positionValidator)
                    .ensureUniqueLabel(name);

            // when & then
            assertThatThrownBy(() -> sut.create(cmd, 1L)).isInstanceOf(ResourceConflictException.class);
            then(positionRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("update position")
    class UpdatePosition {

        @Test
        @DisplayName("early-return if new and original name are identical when renaming")
        void whenRenaming_EarlyReturn_If_NameUnchanged() {
            // given
            Long positionId = 1L;
            Position position = mock(Position.class);

            given(positionFinder.getById(positionId)).willReturn(position);
            given(position.getName()).willReturn("Backend Engineer");

            // when
            sut.update(
                    positionId,
                    new PatchPositionCommand(
                            JsonNullable.of("Backend Engineer"), JsonNullable.undefined(), JsonNullable.undefined()),
                    1L);

            // then
            then(positionValidator).shouldHaveNoInteractions();
            then(position).should(never()).rename(any());
        }
    }

    @Nested
    @DisplayName("delete position")
    class DeletePosition {

        @Test
        @DisplayName("success: unassigns members then deletes the position")
        void successDeletePosition() {
            // given
            Long positionId = 1L;
            Position position = mock(Position.class);

            given(positionFinder.getById(positionId)).willReturn(position);

            // when
            sut.delete(positionId, 1L);

            // then
            then(memberCommandRepository).should().clearPositionAssignments(position);
            then(positionRepository).should().delete(position);
        }
    }
}

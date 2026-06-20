package com.tissue.feature.organization.team.application.service;

import static com.tissue.feature.organization.team.domain.exception.TeamErrorCode.DUPLICATE_TEAM_NAME;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.organization.team.application.dto.request.CreateTeamCommand;
import com.tissue.feature.organization.team.application.dto.request.PatchTeamCommand;
import com.tissue.feature.organization.team.application.port.repository.TeamRepository;
import com.tissue.feature.organization.team.application.service.finder.TeamFinder;
import com.tissue.feature.organization.team.application.service.validator.TeamValidator;
import com.tissue.feature.organization.team.domain.Team;
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

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private TeamFinder teamFinder;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamValidator teamValidator;

    @Mock
    private MemberCommandRepository memberCommandRepository;

    @InjectMocks
    private TeamService sut;

    @Nested
    @DisplayName("create team")
    class CreateTeam {

        @Test
        @DisplayName("success: validates uniqueness and saves the team")
        void successCreateTeam() {
            // given
            Name name = Name.of("Platform");
            Team team = mock(Team.class);

            CreateTeamCommand cmd = CreateTeamCommand.builder()
                    .name(name)
                    .description("platform team")
                    .color(ColorType.ANSI_GREEN)
                    .build();

            given(teamRepository.save(any(Team.class))).willReturn(team);

            // when
            sut.create(cmd, 1L);

            // then
            then(teamValidator).should().ensureUniqueLabel(name);
            then(teamRepository).should().save(any(Team.class));
        }

        @Test
        @DisplayName("fail: throws ResourceConflictException if team name is duplicate")
        void failCreate_If_DuplicateName() {
            // given
            Name name = Name.of("Platform");

            CreateTeamCommand cmd = CreateTeamCommand.builder()
                    .name(name)
                    .description(null)
                    .color(ColorType.ANSI_GREEN)
                    .build();

            willThrow(new ResourceConflictException(DUPLICATE_TEAM_NAME))
                    .given(teamValidator)
                    .ensureUniqueLabel(name);

            // when & then
            assertThatThrownBy(() -> sut.create(cmd, 1L)).isInstanceOf(ResourceConflictException.class);
            then(teamRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("update team")
    class UpdateTeam {

        @Test
        @DisplayName("early-return if new and original name are identical when renaming")
        void whenRenaming_EarlyReturn_If_NameUnchanged() {
            // given
            Long teamId = 1L;
            Team team = mock(Team.class);

            given(teamFinder.getById(teamId)).willReturn(team);
            given(team.getName()).willReturn("Platform");

            // when
            sut.update(
                    teamId,
                    new PatchTeamCommand(
                            JsonNullable.of("Platform"), JsonNullable.undefined(), JsonNullable.undefined()),
                    1L);

            // then
            then(teamValidator).shouldHaveNoInteractions();
            then(team).should(never()).rename(any());
        }
    }

    @Nested
    @DisplayName("delete team")
    class DeleteTeam {

        @Test
        @DisplayName("success: unassigns members then deletes the team")
        void successDeleteTeam() {
            // given
            Long teamId = 1L;
            Team team = mock(Team.class);

            given(teamFinder.getById(teamId)).willReturn(team);

            // when
            sut.delete(teamId, 1L);

            // then
            then(memberCommandRepository).should().clearTeamAssignments(team);
            then(teamRepository).should().delete(team);
        }
    }
}

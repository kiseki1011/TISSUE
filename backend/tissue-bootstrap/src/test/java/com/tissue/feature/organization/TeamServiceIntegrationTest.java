package com.tissue.feature.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.application.port.repository.MemberQueryRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.organization.team.application.dto.request.CreateTeamCommand;
import com.tissue.feature.organization.team.application.dto.response.TeamResponse;
import com.tissue.feature.organization.team.application.port.repository.TeamRepository;
import com.tissue.feature.organization.team.application.service.TeamService;
import com.tissue.feature.organization.team.domain.Team;
import com.tissue.feature.organization.team.domain.exception.TeamErrorCode;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.exception.base.ResourceConflictException;
import com.tissue.shared.vo.Name;
import com.tissue.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class TeamServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private TeamService teamService;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private MemberCommandRepository memberCommandRepository;

    @Autowired
    private MemberQueryRepository memberQueryRepository;

    private Member admin;

    @BeforeEach
    void setUp() {
        admin = memberCommandRepository.save(Member.createAsAdmin("admin@tissue.com", "admin", "Admin"));
        em.flush();
        em.clear();
    }

    @Nested
    @DisplayName("create team")
    class CreateTeam {

        @Test
        @DisplayName("creates a team")
        void successCreateTeam() {
            // given
            CreateTeamCommand cmd = CreateTeamCommand.builder()
                    .name(Name.of("Platform"))
                    .description("platform team")
                    .color(ColorType.GREEN)
                    .build();

            // when
            TeamResponse response = teamService.create(cmd, admin.getId());
            em.flush();
            em.clear();

            // then
            Team team = teamRepository.findById(response.teamId()).orElseThrow();
            assertThat(team.getName()).isEqualTo("Platform");
            assertThat(team.getColor()).isEqualTo(ColorType.GREEN);
        }

        @Test
        @DisplayName("fails if team name already exists")
        void failIfDuplicateName() {
            // given
            teamService.create(
                    CreateTeamCommand.builder()
                            .name(Name.of("Design"))
                            .description(null)
                            .color(ColorType.PINK)
                            .build(),
                    admin.getId());
            em.flush();

            CreateTeamCommand duplicate = CreateTeamCommand.builder()
                    .name(Name.of("Design"))
                    .description(null)
                    .color(ColorType.CYAN)
                    .build();

            // when & then
            assertThatThrownBy(() -> teamService.create(duplicate, admin.getId()))
                    .isInstanceOf(ResourceConflictException.class)
                    .extracting("errorCode")
                    .isEqualTo(TeamErrorCode.DUPLICATE_TEAM_NAME);
        }
    }

    @Nested
    @DisplayName("delete team")
    class DeleteTeam {

        @Test
        @DisplayName("unassigns the team from every member, then deletes it")
        void deleteUnassignsMembers() {
            // given
            Team team = teamRepository.save(Team.create(Name.of("Ops"), "operations", ColorType.ORANGE));
            Member member = memberCommandRepository.save(Member.create("ops@tissue.com", "opsuser", "Ops User"));
            member.assignTeam(team);
            memberCommandRepository.save(member);
            em.flush();
            em.clear();

            // when
            teamService.delete(team.getId(), admin.getId());
            em.flush();
            em.clear();

            // then
            assertThat(teamRepository.findById(team.getId())).isEmpty();
            Member reloaded = memberQueryRepository.findById(member.getId()).orElseThrow();
            assertThat(reloaded.getTeam()).isNull();
        }
    }
}

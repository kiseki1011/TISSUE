package com.tissue.feature.organization;

import static org.assertj.core.api.Assertions.assertThat;

import com.tissue.feature.member.application.dto.MemberProfile;
import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.application.port.repository.MemberQueryRepository;
import com.tissue.feature.member.application.service.MemberAdministrationService;
import com.tissue.feature.member.application.service.MemberProfileCommandService;
import com.tissue.feature.member.application.service.MemberProfileQueryService;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.organization.position.application.port.repository.PositionRepository;
import com.tissue.feature.organization.position.domain.Position;
import com.tissue.feature.organization.team.application.port.repository.TeamRepository;
import com.tissue.feature.organization.team.domain.Team;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.vo.Name;
import com.tissue.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class MemberOrganizationIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MemberProfileCommandService memberProfileCommandService;

    @Autowired
    private MemberAdministrationService memberAdministrationService;

    @Autowired
    private MemberProfileQueryService memberProfileQueryService;

    @Autowired
    private MemberCommandRepository memberCommandRepository;

    @Autowired
    private MemberQueryRepository memberQueryRepository;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Nested
    @DisplayName("self position assignment")
    class AssignOwnPosition {

        @Test
        @DisplayName("assigns and clears the current member's own position")
        void assignAndClear() {
            // given
            Member member = memberCommandRepository.save(Member.create("dev@tissue.com", "dev", "Dev"));
            Position position = positionRepository.save(Position.create(Name.of("Backend"), null, ColorType.ANSI_BLUE));
            em.flush();
            em.clear();

            // when: assign
            memberProfileCommandService.updatePosition(position.getId(), member.getId());
            em.flush();
            em.clear();

            // then
            Member assigned = memberQueryRepository.findById(member.getId()).orElseThrow();
            assertThat(assigned.getPosition()).isNotNull();
            assertThat(assigned.getPosition().getId()).isEqualTo(position.getId());

            // when: clear
            memberProfileCommandService.updatePosition(null, member.getId());
            em.flush();
            em.clear();

            // then
            Member cleared = memberQueryRepository.findById(member.getId()).orElseThrow();
            assertThat(cleared.getPosition()).isNull();
        }
    }

    @Nested
    @DisplayName("admin team assignment")
    class AssignTeam {

        @Test
        @DisplayName("admin assigns and clears a member's team")
        void assignAndClear() {
            // given
            Member admin = memberCommandRepository.save(Member.createAsAdmin("admin@tissue.com", "admin", "Admin"));
            Member target = memberCommandRepository.save(Member.create("dev@tissue.com", "dev", "Dev"));
            Team team = teamRepository.save(Team.create(Name.of("Platform"), null, ColorType.ANSI_GREEN));
            em.flush();
            em.clear();

            // when: assign
            memberAdministrationService.assignTeam(target.getId(), team.getId(), admin.getId());
            em.flush();
            em.clear();

            // then
            Member assigned = memberQueryRepository.findById(target.getId()).orElseThrow();
            assertThat(assigned.getTeam()).isNotNull();
            assertThat(assigned.getTeam().getId()).isEqualTo(team.getId());

            // when: clear
            memberAdministrationService.assignTeam(target.getId(), null, admin.getId());
            em.flush();
            em.clear();

            // then
            Member cleared = memberQueryRepository.findById(target.getId()).orElseThrow();
            assertThat(cleared.getTeam()).isNull();
        }
    }

    @Nested
    @DisplayName("profile reflects position and team")
    class ProfileReflection {

        @Test
        @DisplayName("getMyProfile includes assigned position and team summaries")
        void profileIncludesOrganization() {
            // given
            Member member = memberCommandRepository.save(Member.createAsAdmin("admin@tissue.com", "admin", "Admin"));
            Position position = positionRepository.save(Position.create(Name.of("PM"), null, ColorType.MAGENTA));
            Team team = teamRepository.save(Team.create(Name.of("Core"), null, ColorType.MEDIUMBLUE));
            member.assignPosition(position);
            member.assignTeam(team);
            memberCommandRepository.save(member);
            em.flush();
            em.clear();

            // when
            MemberProfile profile = memberProfileQueryService.getMyProfile(member.getId());

            // then
            assertThat(profile.position()).isNotNull();
            assertThat(profile.position().name()).isEqualTo("PM");
            assertThat(profile.team()).isNotNull();
            assertThat(profile.team().name()).isEqualTo("Core");
        }

        @Test
        @DisplayName("getMyProfile returns null position and team when unassigned")
        void profileNullWhenUnassigned() {
            // given
            Member member = memberCommandRepository.save(Member.createAsAdmin("admin@tissue.com", "admin", "Admin"));
            em.flush();
            em.clear();

            // when
            MemberProfile profile = memberProfileQueryService.getMyProfile(member.getId());

            // then
            assertThat(profile.position()).isNull();
            assertThat(profile.team()).isNull();
        }
    }
}

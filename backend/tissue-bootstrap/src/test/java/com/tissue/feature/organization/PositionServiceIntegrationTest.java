package com.tissue.feature.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.application.port.repository.MemberQueryRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.organization.position.application.dto.request.CreatePositionCommand;
import com.tissue.feature.organization.position.application.dto.response.PositionResponse;
import com.tissue.feature.organization.position.application.port.repository.PositionRepository;
import com.tissue.feature.organization.position.application.service.PositionService;
import com.tissue.feature.organization.position.domain.Position;
import com.tissue.feature.organization.position.domain.exception.PositionErrorCode;
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
class PositionServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private PositionService positionService;

    @Autowired
    private PositionRepository positionRepository;

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
    @DisplayName("create position")
    class CreatePosition {

        @Test
        @DisplayName("creates a position")
        void successCreatePosition() {
            // given
            CreatePositionCommand cmd = CreatePositionCommand.builder()
                    .name(Name.of("Backend Engineer"))
                    .description("backend")
                    .color(ColorType.BLUE)
                    .build();

            // when
            PositionResponse response = positionService.create(cmd, admin.getId());
            em.flush();
            em.clear();

            // then
            Position position =
                    positionRepository.findById(response.positionId()).orElseThrow();
            assertThat(position.getName()).isEqualTo("Backend Engineer");
            assertThat(position.getColor()).isEqualTo(ColorType.BLUE);
        }

        @Test
        @DisplayName("fails if position name already exists")
        void failIfDuplicateName() {
            // given
            positionService.create(
                    CreatePositionCommand.builder()
                            .name(Name.of("Designer"))
                            .description(null)
                            .color(ColorType.PINK)
                            .build(),
                    admin.getId());
            em.flush();

            CreatePositionCommand duplicate = CreatePositionCommand.builder()
                    .name(Name.of("Designer"))
                    .description(null)
                    .color(ColorType.CYAN)
                    .build();

            // when & then
            assertThatThrownBy(() -> positionService.create(duplicate, admin.getId()))
                    .isInstanceOf(ResourceConflictException.class)
                    .extracting("errorCode")
                    .isEqualTo(PositionErrorCode.DUPLICATE_POSITION_NAME);
        }
    }

    @Nested
    @DisplayName("delete position")
    class DeletePosition {

        @Test
        @DisplayName("unassigns the position from every member, then deletes it")
        void deleteUnassignsMembers() {
            // given
            Position position = positionRepository.save(Position.create(Name.of("QA"), "quality", ColorType.YELLOW));
            Member member = memberCommandRepository.save(Member.create("qa@tissue.com", "qauser", "QA User"));
            member.assignPosition(position);
            memberCommandRepository.save(member);
            em.flush();
            em.clear();

            // when
            positionService.delete(position.getId(), admin.getId());
            em.flush();
            em.clear();

            // then
            assertThat(positionRepository.findById(position.getId())).isEmpty();
            Member reloaded = memberQueryRepository.findById(member.getId()).orElseThrow();
            assertThat(reloaded.getPosition()).isNull();
        }
    }
}

package com.tissue.feature.agent.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.agent.model.application.dto.request.CreateAiModelCommand;
import com.tissue.feature.agent.model.application.dto.response.AiModelResponse;
import com.tissue.feature.agent.model.application.port.repository.AiModelRepository;
import com.tissue.feature.agent.model.application.service.AiModelService;
import com.tissue.feature.agent.model.domain.AiModel;
import com.tissue.feature.agent.model.domain.exception.AiModelErrorCode;
import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.application.port.repository.MemberQueryRepository;
import com.tissue.feature.member.domain.Member;
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
class AiModelServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private AiModelService aiModelService;

    @Autowired
    private AiModelRepository aiModelRepository;

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
    @DisplayName("create model")
    class CreateModel {

        @Test
        @DisplayName("creates an AI model")
        void successCreateModel() {
            // given
            CreateAiModelCommand cmd = CreateAiModelCommand.builder()
                    .name(Name.of("claude-opus-4-8"))
                    .description("flagship")
                    .color(ColorType.ANSI_BLUE)
                    .build();

            // when
            AiModelResponse response = aiModelService.create(cmd, admin.getId());
            em.flush();
            em.clear();

            // then
            AiModel model = aiModelRepository.findById(response.modelId()).orElseThrow();
            assertThat(model.getName()).isEqualTo("claude-opus-4-8");
            assertThat(model.getColor()).isEqualTo(ColorType.ANSI_BLUE);
        }

        @Test
        @DisplayName("fails if model name already exists")
        void failIfDuplicateName() {
            // given
            aiModelService.create(
                    CreateAiModelCommand.builder()
                            .name(Name.of("gpt-9"))
                            .description(null)
                            .color(ColorType.PINK)
                            .build(),
                    admin.getId());
            em.flush();

            CreateAiModelCommand duplicate = CreateAiModelCommand.builder()
                    .name(Name.of("gpt-9"))
                    .description(null)
                    .color(ColorType.ANSI_CYAN)
                    .build();

            // when & then
            assertThatThrownBy(() -> aiModelService.create(duplicate, admin.getId()))
                    .isInstanceOf(ResourceConflictException.class)
                    .extracting("errorCode")
                    .isEqualTo(AiModelErrorCode.DUPLICATE_AI_MODEL_NAME);
        }
    }

    @Nested
    @DisplayName("delete model")
    class DeleteModel {

        @Test
        @DisplayName("unassigns the model from every member, then deletes it")
        void deleteUnassignsMembers() {
            // given
            AiModel model =
                    aiModelRepository.save(AiModel.create(Name.of("gemini-9"), "vision", ColorType.ANSI_YELLOW));
            Member member = memberCommandRepository.save(Member.create("bot@tissue.com", "botuser", "Bot User"));
            member.assignModel(model);
            memberCommandRepository.save(member);
            em.flush();
            em.clear();

            // when
            aiModelService.delete(model.getId(), admin.getId());
            em.flush();
            em.clear();

            // then
            assertThat(aiModelRepository.findById(model.getId())).isEmpty();
            Member reloaded = memberQueryRepository.findById(member.getId()).orElseThrow();
            assertThat(reloaded.getModel()).isNull();
        }
    }
}

package com.tissue.feature.project.application.service.validator;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.tissue.feature.project.application.port.repository.ProjectQueryRepository;
import com.tissue.feature.project.domain.exception.DuplicateProjectKeyException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProjectValidatorTest {

    private final ProjectQueryRepository projectRepository = mock(ProjectQueryRepository.class);
    private final ProjectValidator sut = new ProjectValidator(projectRepository);

    @Test
    @DisplayName("passes when the project key is globally unique")
    void passesWhenGloballyUnique() {
        given(projectRepository.existsByKey("PROJ")).willReturn(false);

        assertThatCode(() -> sut.ensureUniqueProjectKey("PROJ")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("rejects a globally duplicate project key")
    void rejectsGlobalDuplicate() {
        given(projectRepository.existsByKey("PROJ")).willReturn(true);

        assertThatThrownBy(() -> sut.ensureUniqueProjectKey("PROJ")).isInstanceOf(DuplicateProjectKeyException.class);
    }
}

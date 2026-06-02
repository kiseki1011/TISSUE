package com.tissue.admin.application.port.usecase;

import com.tissue.feature.project.application.dto.response.ProjectHardDeletePreview;

public interface AdminProjectUseCase {

    ProjectHardDeletePreview previewHardDelete(String projectKey);

    ProjectHardDeletePreview hardDelete(String projectKey, String confirmationKey, Long actorMemberId);
}

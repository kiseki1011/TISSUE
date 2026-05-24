package com.tissue.feature.wiki.application.port.usecase;

import com.tissue.feature.wiki.application.dto.response.WikiBookmarkResponse;
import java.util.List;

public interface WikiBookmarkQueryUseCase {

    List<WikiBookmarkResponse> getBookmarks(String workspaceKey, Long actorMemberId);
}

package com.tissue.feature.wiki.application.port.usecase;

import com.tissue.feature.wiki.application.dto.request.AttachWikiTagCommand;
import com.tissue.feature.wiki.application.dto.response.WikiTagResponse;

public interface WikiTagCommandUseCase {

    WikiTagResponse attachTag(Long wikiId, AttachWikiTagCommand cmd, Long actorMemberId);

    void detachTag(Long wikiId, Long tagId, Long actorMemberId);
}

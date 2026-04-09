package com.tissue.feature.wiki.application.dto.response;

import com.tissue.feature.wiki.domain.WikiLink;
import com.tissue.feature.wiki.domain.enums.WikiLinkTargetType;

public record WikiLinkInfo(Long linkId, WikiLinkTargetType targetType, Long targetId) {

    public static WikiLinkInfo from(WikiLink link) {
        return new WikiLinkInfo(link.getId(), link.getTargetType(), link.getTargetId());
    }
}

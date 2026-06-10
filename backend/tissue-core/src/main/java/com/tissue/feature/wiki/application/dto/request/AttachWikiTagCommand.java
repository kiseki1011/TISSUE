package com.tissue.feature.wiki.application.dto.request;

import com.tissue.shared.enums.ColorType;
import com.tissue.shared.vo.Name;
import org.jspecify.annotations.Nullable;

public record AttachWikiTagCommand(Name name, @Nullable ColorType color) {}

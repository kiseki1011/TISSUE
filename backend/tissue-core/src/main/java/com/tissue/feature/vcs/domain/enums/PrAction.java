package com.tissue.feature.vcs.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PrAction {
    OPENED,
    CLOSED,
    REOPENED,
    MERGED,
    UNKNOWN;
}

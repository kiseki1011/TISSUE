package com.tissue.shared.enums;

import java.util.Locale;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SupportedLanguage {
    KO(Locale.KOREA),
    EN(Locale.ENGLISH);

    private final Locale locale;
}

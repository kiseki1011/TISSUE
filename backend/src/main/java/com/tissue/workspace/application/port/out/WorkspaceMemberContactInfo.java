package com.tissue.workspace.application.port.out;

import com.tissue.common.enums.SupportedLanguage;

public interface WorkspaceMemberContactInfo {
    Long getMemberId();

    String getEmail();

    SupportedLanguage getLanguage();
}

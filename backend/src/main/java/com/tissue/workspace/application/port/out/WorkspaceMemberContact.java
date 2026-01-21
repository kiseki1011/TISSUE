package com.tissue.workspace.application.port.out;

import com.tissue.common.enums.SupportedLanguage;

public record WorkspaceMemberContact(Long memberId, String email, SupportedLanguage language) {}

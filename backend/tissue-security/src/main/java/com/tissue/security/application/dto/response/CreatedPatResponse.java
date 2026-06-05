package com.tissue.security.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of issuing a token. The secret is shown exactly once")
public record CreatedPatResponse(
        @Schema(description = "The secret token. It cannot be retrieved again.")
        String token,

        PatResponse pat) {

    public static CreatedPatResponse of(String token, PatResponse pat) {
        return new CreatedPatResponse(token, pat);
    }
}

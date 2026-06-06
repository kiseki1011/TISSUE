package com.tissue.admin.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

@Schema(description = "Instance-wide operational summary for operators")
@Builder
public record AdminSystemInfo(
        @Schema(description = "Tissue server version", example = "1.0.0")
        String version,

        @Schema(description = "Server display name", example = "Tissue Server")
        String serverName,

        @Schema(description = "Active Spring profiles", example = "[\"prod\"]")
        List<String> activeProfiles,

        @Schema(description = "Whether Redis/Valkey caching is enabled", example = "true")
        boolean redisEnabled,

        @Schema(
                description = "Whether the instance has been seeded (at least one member exists, so default "
                        + "workflows/issue types were created on first signup)",
                example = "true")
        boolean seeded,

        @Schema(description = "Member counts by status") MemberStats members) {

    @Schema(description = "Member counts by status")
    @Builder
    public record MemberStats(
            @Schema(example = "120") long total,
            @Schema(example = "98") long active,
            @Schema(example = "2") long locked,
            @Schema(example = "18") long deleted,
            @Schema(example = "2") long purged,

            @Schema(description = "Active members with the SUPER_ADMIN role", example = "1")
            long activeSuperAdmins) {}
}

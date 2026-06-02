package com.tissue.admin.adapter.web;

import com.tissue.admin.application.dto.AdminAuditLogResponse;
import com.tissue.admin.application.port.usecase.AdminAuditQueryUseCase;
import com.tissue.admin.domain.AdminAuditAction;
import com.tissue.admin.domain.AdminAuditTargetType;
import com.tissue.shared.auth.RequireSuperAdmin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Super Admin Audit")
@RestController
@RequestMapping("/api/v1/admin/audit")
@RequiredArgsConstructor
@RequireSuperAdmin
public class AdminAuditController {

    private final AdminAuditQueryUseCase adminAuditQueryUseCase;

    @Operation(operationId = "adminListAuditLogs", summary = "List admin audit log", description = """
                List the permanent audit trail of privileged SUPER_ADMIN actions (ex: role changes,
                force-withdraw/restore, session revocation, ...). Optional `actorMemberId`, `action`,
                and `targetType` filters; newest first.

                **Requirements:**
                - Requires system `SUPER_ADMIN` role""")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Audit log retrieved")})
    @GetMapping
    public ResponseEntity<Page<AdminAuditLogResponse>> listAuditLogs(
            @RequestParam(required = false) @Nullable Long actorMemberId,
            @RequestParam(required = false) @Nullable AdminAuditAction action,
            @RequestParam(required = false) @Nullable AdminAuditTargetType targetType,
            Pageable pageable) {
        Page<AdminAuditLogResponse> response =
                adminAuditQueryUseCase.listAuditLogs(actorMemberId, action, targetType, pageable);
        return ResponseEntity.ok(response);
    }
}

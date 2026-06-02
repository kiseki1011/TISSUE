package com.tissue.admin.adapter.web;

import com.tissue.admin.application.dto.AdminSystemInfo;
import com.tissue.admin.application.port.usecase.AdminSystemInfoUseCase;
import com.tissue.shared.auth.RequireSuperAdmin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Advanced System Info")
@RestController
@RequestMapping("/api/v1/admin/system-info")
@RequiredArgsConstructor
@RequireSuperAdmin
public class AdminSystemController {

    private final AdminSystemInfoUseCase adminSystemInfoUseCase;

    @Operation(operationId = "adminGetSystemInfo", summary = "Get instance operational summary", description = """
                Operator view of the running instance: version, active profiles, Redis status, seeding status,
                and member counts by status (including active SUPER_ADMIN count). For live metrics (CPU, latency)
                use the observability stack; this is a quick product-level glance.

                **Requirements:**
                - Requires system `SUPER_ADMIN` role""")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "System info retrieved")})
    @GetMapping
    public ResponseEntity<AdminSystemInfo> getSystemInfo() {
        return ResponseEntity.ok(adminSystemInfoUseCase.getSystemInfo());
    }
}

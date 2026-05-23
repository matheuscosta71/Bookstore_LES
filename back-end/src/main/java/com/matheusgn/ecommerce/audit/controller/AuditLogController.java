package com.matheusgn.ecommerce.audit.controller;

import com.matheusgn.ecommerce.audit.dto.AuditLogResponse;
import com.matheusgn.ecommerce.audit.entity.AuditLog;
import com.matheusgn.ecommerce.audit.service.AuditLogService;
import com.matheusgn.ecommerce.config.PageConstraints;
import com.matheusgn.ecommerce.sales.service.AdminOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static com.matheusgn.ecommerce.config.PageConstraints.DEFAULT_PAGE_SIZE;

@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Admin — auditoria")
public class AuditLogController {

    private final AdminOrderService adminOrderService;
    private final AuditLogService auditLogService;

    @GetMapping
    @Operation(summary = "Listar logs de auditoria")
    public ResponseEntity<Page<AuditLogResponse>> list(
            @RequestHeader("X-Admin-Key") String adminKey,
            @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        adminOrderService.assertAdmin(adminKey);
        Page<AuditLogResponse> page = auditLogService.list(PageConstraints.clamp(pageable))
                .map(this::toResponse);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{entityName}/{entityId}")
    @Operation(summary = "Logs por entidade")
    public ResponseEntity<List<AuditLogResponse>> byEntity(
            @RequestHeader("X-Admin-Key") String adminKey,
            @PathVariable String entityName,
            @PathVariable UUID entityId) {
        adminOrderService.assertAdmin(adminKey);
        return ResponseEntity.ok(
                auditLogService.findByEntity(entityName, entityId).stream().map(this::toResponse).toList());
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .entityName(log.getEntityName())
                .entityId(log.getEntityId())
                .actionType(log.getActionType())
                .changedBy(log.getChangedBy())
                .changedAt(log.getChangedAt())
                .changedData(log.getChangedData())
                .build();
    }
}

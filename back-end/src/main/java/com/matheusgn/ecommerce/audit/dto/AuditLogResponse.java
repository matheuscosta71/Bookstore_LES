package com.matheusgn.ecommerce.audit.dto;

import com.matheusgn.ecommerce.audit.entity.AuditActionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogResponse {

    private UUID id;
    private String entityName;
    private UUID entityId;
    private AuditActionType actionType;
    private String changedBy;
    private Instant changedAt;
    private String changedData;
}

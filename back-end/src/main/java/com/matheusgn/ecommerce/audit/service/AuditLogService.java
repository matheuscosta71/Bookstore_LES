package com.matheusgn.ecommerce.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.matheusgn.ecommerce.audit.entity.AuditActionType;
import com.matheusgn.ecommerce.audit.entity.AuditLog;
import com.matheusgn.ecommerce.audit.repository.AuditLogRepository;
import com.matheusgn.ecommerce.audit.support.AuditActorResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final AuditActorResolver auditActorResolver;
    private final ObjectMapper objectMapper;

    @Transactional
    public void logCreate(String entityName, UUID entityId, Object payload) {
        persist(AuditActionType.CREATE, entityName, entityId, payload);
    }

    @Transactional
    public void logUpdate(String entityName, UUID entityId, Object payload) {
        persist(AuditActionType.UPDATE, entityName, entityId, payload);
    }

    private void persist(AuditActionType action, String entityName, UUID entityId, Object payload) {
        String json = toMaskedJson(payload);
        AuditLog log = AuditLog.builder()
                .entityName(entityName)
                .entityId(entityId)
                .actionType(action)
                .changedBy(auditActorResolver.resolveActor())
                .changedAt(Instant.now())
                .changedData(json)
                .build();
        auditLogRepository.save(log);
    }

    private String toMaskedJson(Object payload) {
        if (payload == null) {
            return null;
        }
        try {
            JsonNode node = objectMapper.valueToTree(payload);
            maskSecrets(node);
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"serialization\"}";
        }
    }

    private void maskSecrets(JsonNode node) {
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            Iterator<String> fields = obj.fieldNames();
            while (fields.hasNext()) {
                String name = fields.next();
                if (name.toLowerCase().contains("password")) {
                    obj.put(name, "***");
                } else {
                    maskSecrets(obj.get(name));
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                maskSecrets(child);
            }
        }
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> list(Pageable pageable) {
        return auditLogRepository.findAllByOrderByChangedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> findByEntity(String entityName, UUID entityId) {
        return auditLogRepository.findByEntityNameAndEntityIdOrderByChangedAtDesc(entityName, entityId);
    }
}

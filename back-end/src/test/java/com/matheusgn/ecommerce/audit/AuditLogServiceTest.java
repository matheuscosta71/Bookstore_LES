package com.matheusgn.ecommerce.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matheusgn.ecommerce.audit.entity.AuditActionType;
import com.matheusgn.ecommerce.audit.entity.AuditLog;
import com.matheusgn.ecommerce.audit.repository.AuditLogRepository;
import com.matheusgn.ecommerce.audit.service.AuditLogService;
import com.matheusgn.ecommerce.audit.support.AuditActorResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private AuditActorResolver auditActorResolver;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AuditLogService auditLogService;

    @Test
    void logCreate_persistsChangedByAndMaskedPassword() {
        when(auditActorResolver.resolveActor()).thenReturn("admin@test");

        UUID id = UUID.randomUUID();
        auditLogService.logCreate("Customer", id, Map.of("password", "secret", "email", "a@b.com"));

        ArgumentCaptor<AuditLog> cap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(cap.capture());
        assertThat(cap.getValue().getChangedBy()).isEqualTo("admin@test");
        assertThat(cap.getValue().getChangedData()).contains("***");
        assertThat(cap.getValue().getChangedData()).contains("a@b.com");
    }

    @Test
    void logUpdate_persistsActionUpdateAndMaskedPassword() {
        when(auditActorResolver.resolveActor()).thenReturn("operator@corp");

        UUID id = UUID.randomUUID();
        auditLogService.logUpdate("Customer", id, Map.of("password", "plain", "fullName", "Maria"));

        ArgumentCaptor<AuditLog> cap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(cap.capture());
        assertThat(cap.getValue().getActionType()).isEqualTo(AuditActionType.UPDATE);
        assertThat(cap.getValue().getChangedBy()).isEqualTo("operator@corp");
        assertThat(cap.getValue().getChangedData()).contains("***");
        assertThat(cap.getValue().getChangedData()).contains("Maria");
    }
}

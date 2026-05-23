package com.matheusgn.ecommerce.audit.repository;

import com.matheusgn.ecommerce.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findAllByOrderByChangedAtDesc(Pageable pageable);

    List<AuditLog> findByEntityNameAndEntityIdOrderByChangedAtDesc(String entityName, UUID entityId);
}

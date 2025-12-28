package com.kissanmitra.repository;

import com.kissanmitra.entity.AuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for AuditLog entity.
 */
@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {

    /**
     * Finds audit logs by entity type and ID.
     *
     * @param entityType entity type
     * @param entityId entity ID
     * @return list of audit logs
     */
    List<AuditLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(String entityType, String entityId);
}


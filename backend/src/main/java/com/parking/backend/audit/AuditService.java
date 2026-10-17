package com.parking.backend.audit;

import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository repo;

    public AuditService(AuditLogRepository repo) {
        this.repo = repo;
    }

    public void log(String actorEmail, String action, String targetType, Long targetId, String details) {
        AuditLog a = new AuditLog();
        a.setActorEmail(actorEmail);
        a.setAction(action);
        a.setTargetType(targetType);
        a.setTargetId(targetId);
        a.setDetails(details);
        repo.save(a);
    }
}
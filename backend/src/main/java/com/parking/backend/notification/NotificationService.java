package com.parking.backend.notification;

import com.parking.backend.common.NotFoundException;
import com.parking.backend.user.User;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository repo;

    public NotificationService(NotificationRepository repo) {
        this.repo = repo;
    }

    /** Joins the caller's transaction, so it rolls back together with it. */
    public void create(User user, String type, String message) {
        Notification n = new Notification();
        n.setUser(user);
        n.setType(type);
        n.setMessage(message);
        repo.save(n);
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> list(String email) {
        return repo.findByUserEmailOrderByCreatedAtDesc(email).stream().map(this::toDto).toList();
    }

    @Transactional
    public void markRead(String email, Long id) {
        Notification n = repo.findByIdAndUserEmail(id, email)
                .orElseThrow(() -> new NotFoundException("Notification " + id + " not found"));
        n.setRead(true);
    }

    private NotificationDto toDto(Notification n) {
        return new NotificationDto(n.getId(), n.getType(), n.getMessage(), n.isRead(), n.getCreatedAt());
    }
}
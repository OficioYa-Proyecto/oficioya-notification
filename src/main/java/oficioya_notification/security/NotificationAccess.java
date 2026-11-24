package oficioya_notification.security;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import oficioya_notification.repository.NotificationRepository;

/** Reglas de "es el dueño" usadas desde @PreAuthorize. */
@Component("notificationAccess")
public class NotificationAccess {

    private final NotificationRepository notificationRepository;

    public NotificationAccess(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public boolean isSelf(UUID userId, Authentication authentication) {
        if (userId == null || authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return userId.toString().equalsIgnoreCase(String.valueOf(authentication.getName()));
    }

    public boolean ownsNotification(UUID notificationId, Authentication authentication) {
        if (notificationId == null) {
            return false;
        }
        return notificationRepository.findById(notificationId)
                .map(n -> isSelf(n.getReceiverId(), authentication))
                .orElse(false);
    }
}
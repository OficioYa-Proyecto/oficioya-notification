package oficioya_notification.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import oficioya_notification.dto.EmailRequest;
import oficioya_notification.dto.NotificationRequest;
import oficioya_notification.dto.NotificationResponse;
import oficioya_notification.entity.Notification;
import oficioya_notification.mapper.ResponseMapper;
import oficioya_notification.model.NotificationType;
import oficioya_notification.repository.NotificationRepository;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ResponseMapper responseMapper;
    private final FcmService fcmService;
    private final EmailNotificationService emailNotificationService;

    public NotificationService(NotificationRepository notificationRepository, ResponseMapper responseMapper,
                               FcmService fcmService, EmailNotificationService emailNotificationService) {
        this.notificationRepository = notificationRepository;
        this.responseMapper = responseMapper;
        this.fcmService = fcmService;
        this.emailNotificationService = emailNotificationService;
    }

    public List<NotificationResponse> findAllForUser(UUID userId) {
        return notificationRepository.findByReceiverId(userId).stream().map(responseMapper::toResponse).toList();
    }

    public Optional<NotificationResponse> findById(UUID id) {
        Optional<Notification> response = notificationRepository.findById(id);
        if (response.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(responseMapper.toResponse(response.orElse(null)));
        }
    }

    public List<NotificationResponse> findAllForARequest(UUID requestId, UUID receiverId) {
        return notificationRepository.findByRequestIdAndReceiverId(requestId, receiverId).stream().map(responseMapper::toResponse).toList();
    }

    public NotificationResponse sendNotification(NotificationRequest request) {
        Notification noti = responseMapper.toNotification(request);
        noti.setDateSent(noti.getDateCreated());

        NotificationResponse response = responseMapper.toResponse(notificationRepository.save(noti));

        String deviceToken = request.deviceToken();
        String topic = (deviceToken == null || deviceToken.isBlank()) ? "user_" + request.receiverId() : null;
        fcmService.sendPush(deviceToken, topic,
                noti.getTitle(), noti.getDescription(),
                java.util.Map.of("type", String.valueOf(request.type()), "requestId", String.valueOf(request.requestId())));

        sendEmailIfRequested(request, noti);

        return response;
    }

    private void sendEmailIfRequested(NotificationRequest request, Notification noti) {
        String email = request.receiverEmail();
        if (email == null || email.isBlank()) {
            return;
        }
        String subject = request.type() == NotificationType.REQUEST_CREATED
                ? "Tu solicitud en OficioYa fue creada"
                : (noti.getTitle() != null ? noti.getTitle() : "Notificación OficioYa");
        String body = (noti.getDescription() != null ? noti.getDescription() : "")
                + "\n\nID de solicitud: " + noti.getRequestId()
                + "\n\nOficioYa";
        emailNotificationService.sendEmail(new EmailRequest(email, subject, body, String.valueOf(request.type())));
    }

    public boolean deleteNotification(UUID requestId) {
        if (!notificationRepository.existsById(requestId)) {
            return false;
        }
        notificationRepository.deleteById(requestId);
        return true;
    }
}
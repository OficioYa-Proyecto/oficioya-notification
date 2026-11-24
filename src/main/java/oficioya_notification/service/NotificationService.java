package oficioya_notification.service;

import java.util.ArrayList;
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
    private final NotificationEmailTemplate emailTemplate;

    public NotificationService(NotificationRepository notificationRepository, ResponseMapper responseMapper,
                               FcmService fcmService, EmailNotificationService emailNotificationService,
                               NotificationEmailTemplate emailTemplate) {
        this.notificationRepository = notificationRepository;
        this.responseMapper = responseMapper;
        this.fcmService = fcmService;
        this.emailNotificationService = emailNotificationService;
        this.emailTemplate = emailTemplate;
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

        List<String> deliveryErrors = new ArrayList<>();
        String deviceToken = request.deviceToken();
        String topic = (deviceToken == null || deviceToken.isBlank()) ? "user_" + request.receiverId() : null;
        String pushError = fcmService.sendPush(deviceToken, topic,
                noti.getTitle(), noti.getDescription(),
                java.util.Map.of("type", String.valueOf(request.type()), "requestId", String.valueOf(request.requestId())));
        if (pushError != null && !pushError.isBlank()) {
            deliveryErrors.add("FCM: " + pushError);
        }

        deliveryErrors.addAll(sendEmailIfRequested(request, noti));
        if (!deliveryErrors.isEmpty()) {
            emailNotificationService.sendFailureReport(
                    String.valueOf(request.type()),
                    String.valueOf(request.receiverEmail()),
                    String.join(" | ", deliveryErrors));
        }

        return response;
    }

    private List<String> sendEmailIfRequested(NotificationRequest request, Notification noti) {
        String email = request.receiverEmail();
        if (email == null || email.isBlank()) {
            return List.of("Correo del destinatario vacío; no se envió el correo. Revise la resolución de correos en Service.");
        }
        if (request.type() == NotificationType.REQUEST_CREATED) {
            String subject = "Tu solicitud en OficioYa fue creada";
            String plain = "Solicitud enviada\nLos trabajadores disponibles ya fueron notificados.\n\n"
                    + "ID de solicitud: " + noti.getRequestId() + "\n\nOficioYa";
            emailNotificationService.sendHtmlEmail(
                    new EmailRequest(email, subject, plain, "REQUEST_CREATED"),
                    emailTemplate.contractorHtml(request, noti));
            return List.of();
        }
        if (request.type() == NotificationType.INCOMING_REQUEST) {
            String subject = "Solicitud entrante en OficioYa";
            emailNotificationService.sendHtmlEmail(
                    new EmailRequest(email, subject, emailTemplate.plainText(request, noti), "INCOMING_REQUEST"),
                    emailTemplate.workerHtml(request, noti));
            return List.of();
        }
        String subject = noti.getTitle() != null ? noti.getTitle() : "Notificación OficioYa";
        String body = (noti.getDescription() != null ? noti.getDescription() : "")
                + "\n\nID de solicitud: " + noti.getRequestId()
                + "\n\nOficioYa";
        emailNotificationService.sendEmail(new EmailRequest(email, subject, body, String.valueOf(request.type())));
        return List.of();
    }

    public boolean deleteNotification(UUID requestId) {
        if (!notificationRepository.existsById(requestId)) {
            return false;
        }
        notificationRepository.deleteById(requestId);
        return true;
    }
}
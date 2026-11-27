package oficioya_notification.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import oficioya_notification.dto.EmailRequest;
import oficioya_notification.dto.NotificationRequest;
import oficioya_notification.dto.NotificationResponse;
import oficioya_notification.entity.Notification;
import oficioya_notification.exception.EmailDeliveryException;
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

        String deviceToken = request.deviceToken();
        String topic = (deviceToken == null || deviceToken.isBlank()) ? "user_" + request.receiverId() : null;
        fcmService.sendPush(deviceToken, topic,
                noti.getTitle(), noti.getDescription(),
                java.util.Map.of("type", String.valueOf(request.type()), "requestId", String.valueOf(request.requestId())));

        // Si el correo falla se propaga EmailDeliveryException: la API responde error.
        sendEmailIfRequested(request, noti);

        return response;
    }

    private void sendEmailIfRequested(NotificationRequest request, Notification noti) {
        String email = request.receiverEmail();
        if (email == null || email.isBlank()) {
            throw new EmailDeliveryException(
                    "Correo del destinatario vacío; no se envió el correo. Revise la resolución de correos en Service.");
        }
        boolean hasCarousel = request.photoBase64s() != null && !request.photoBase64s().isEmpty();
        if (request.type() == NotificationType.REQUEST_CREATED) {
            String subject = "Tu solicitud en OficioYa fue creada";
            String plain = "Solicitud enviada\nLos trabajadores disponibles ya fueron notificados.\n\n"
                    + "ID de solicitud: " + noti.getRequestId() + "\n\nOficioYa";
            if (hasCarousel) {
                emailNotificationService.sendHtmlEmailWithPhotos(
                        new EmailRequest(email, subject, plain, "REQUEST_CREATED"),
                        emailTemplate.contractorHtml(request, noti),
                        request.photoBase64s(), request.photoContentTypes());
            } else {
                emailNotificationService.sendHtmlEmail(
                        new EmailRequest(email, subject, plain, "REQUEST_CREATED"),
                        emailTemplate.contractorHtml(request, noti));
            }
            return;
        }
        if (request.type() == NotificationType.INCOMING_REQUEST) {
            String subject = "Solicitud entrante en OficioYa";
            EmailRequest mail = new EmailRequest(email, subject,
                    emailTemplate.plainText(request, noti), "INCOMING_REQUEST");
            if (hasCarousel) {
                emailNotificationService.sendHtmlEmailWithPhotos(mail, emailTemplate.workerHtml(request, noti),
                        request.photoBase64s(), request.photoContentTypes());
            } else {
                emailNotificationService.sendHtmlEmail(mail, emailTemplate.workerHtml(request, noti),
                        request.imageBase64(), request.imageContentType());
            }
            return;
        }
        EmailRequest mail = new EmailRequest(email,
                emailTemplate.statusSubject(request, noti),
                emailTemplate.statusPlain(request, noti),
                String.valueOf(request.type()));
        String html = emailTemplate.statusHtml(request, noti);
        if (hasCarousel) {
            emailNotificationService.sendHtmlEmailWithPhotos(mail, html,
                    request.photoBase64s(), request.photoContentTypes());
        } else if (request.imageBase64() != null && !request.imageBase64().isBlank()) {
            emailNotificationService.sendHtmlEmail(mail, html,
                    request.imageBase64(), request.imageContentType());
        } else {
            emailNotificationService.sendHtmlEmail(mail, html);
        }
    }

    public boolean deleteNotification(UUID requestId) {
        if (!notificationRepository.existsById(requestId)) {
            return false;
        }
        notificationRepository.deleteById(requestId);
        return true;
    }
}

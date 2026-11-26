package oficioya_notification.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import oficioya_notification.model.NotificationType;

/**
 * Datos necesarios para persistir la notificación y para construir el correo.
 * Los campos de la solicitud son opcionales para mantener compatibilidad con
 * productores que todavía no envían el contexto completo.
 *
 * locality es la localidad elegida en el formulario; neighborhood es el barrio
 * escrito por el contratante. Ambos viajan por separado en el correo.
 *
 * acceptUrl y rejectUrl son enlaces firmados generados por Service: al pulsarlos
 * se ejecuta la acción directamente y se redirige al front.
 *
 * photoBase64s/photoContentTypes alimentan el carrusel del correo; imageBase64
 * se conserva para productores que todavía envían una sola foto.
 */
public record NotificationRequest (
    UUID receiverId,
    String title,
    String description,
    String message,
    NotificationType type,
    UUID requestId,
    LocalDateTime dateCreated,
    String deviceToken,
    String receiverEmail,
    LocalDateTime scheduledDateTime,
    String locality,
    String neighborhood,
    String exactAddress,
    String imageUrl,
    String acceptUrl,
    String rejectUrl,
    String imageBase64,
    String imageContentType,
    List<String> photoBase64s,
    List<String> photoContentTypes
    ){

    public NotificationRequest(UUID receiverId, String title, String description, String message,
                               NotificationType type, UUID requestId, LocalDateTime dateCreated, String deviceToken) {
        this(receiverId, title, description, message, type, requestId, dateCreated, deviceToken,
                null, null, null, null, null, null, null, null, null, null, null, null);
    }

    public NotificationRequest(UUID receiverId, String title, String description, String message,
                               NotificationType type, UUID requestId, LocalDateTime dateCreated, String deviceToken,
                               String receiverEmail) {
        this(receiverId, title, description, message, type, requestId, dateCreated, deviceToken,
                receiverEmail, null, null, null, null, null, null, null, null, null, null, null);
    }

    public NotificationRequest(UUID receiverId, String title, String description, String message,
                               NotificationType type, UUID requestId, LocalDateTime dateCreated, String deviceToken,
                               String receiverEmail, LocalDateTime scheduledDateTime, String neighborhood,
                               String exactAddress, String imageUrl) {
        this(receiverId, title, description, message, type, requestId, dateCreated, deviceToken,
                receiverEmail, scheduledDateTime, null, neighborhood, exactAddress, imageUrl,
                null, null, null, null, null, null);
    }

    public NotificationRequest(UUID receiverId, String title, String description, String message,
                               NotificationType type, UUID requestId, LocalDateTime dateCreated, String deviceToken,
                               String receiverEmail, LocalDateTime scheduledDateTime, String neighborhood,
                               String exactAddress, String imageUrl, String acceptUrl, String rejectUrl) {
        this(receiverId, title, description, message, type, requestId, dateCreated, deviceToken,
                receiverEmail, scheduledDateTime, null, neighborhood, exactAddress, imageUrl,
                acceptUrl, rejectUrl, null, null, null, null);
    }

    /** Constructor anterior, sin localidad, para productores que todavía no la envían. */
    public NotificationRequest(UUID receiverId, String title, String description, String message,
                               NotificationType type, UUID requestId, LocalDateTime dateCreated, String deviceToken,
                               String receiverEmail, LocalDateTime scheduledDateTime, String neighborhood,
                               String exactAddress, String imageUrl, String acceptUrl, String rejectUrl,
                               String imageBase64, String imageContentType,
                               List<String> photoBase64s, List<String> photoContentTypes) {
        this(receiverId, title, description, message, type, requestId, dateCreated, deviceToken,
                receiverEmail, scheduledDateTime, null, neighborhood, exactAddress, imageUrl,
                acceptUrl, rejectUrl, imageBase64, imageContentType, photoBase64s, photoContentTypes);
    }
}

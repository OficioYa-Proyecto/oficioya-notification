package oficioya_notification.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import oficioya_notification.dto.NotificationRequest;
import oficioya_notification.entity.Notification;
import oficioya_notification.model.NotificationType;

class NotificationEmailTemplateTest {

    private final NotificationEmailTemplate template = new NotificationEmailTemplate();

    @Test
    void workerHtmlContieneLosElementosDeLaTarjeta() {
        NotificationRequest request = new NotificationRequest(
                UUID.randomUUID(), "Solicitud entrante", "Fuga de agua", "msg",
                NotificationType.INCOMING_REQUEST, UUID.randomUUID(), null, null, "trabajador@correo.com",
                LocalDateTime.of(2026, 3, 2, 10, 30), "Laureles", "Cra. 74 # 39-25", null,
                "https://service.test/api/v1/requests/email/accept?requestId=1&token=abc",
                "https://service.test/api/v1/requests/email/reject?requestId=1&token=def");
        Notification notification = new Notification();
        notification.setDescription("Fuga de agua");
        notification.setRequestId(request.requestId());

        String html = template.workerHtml(request, notification);

        assertTrue(html.contains("Alerta clara."));
        assertTrue(html.contains("Solicitud entrante"));
        assertTrue(html.contains("Nueva reparación cerca de ti"));
        assertTrue(html.contains("OficioYa"));
        assertTrue(html.contains("#dc623f"));
        assertTrue(html.contains("#fffcf7"));
        assertTrue(html.contains("#e4ded4"));
        assertTrue(html.contains("cid:oficioya-logo"));
        assertTrue(html.contains("Laureles"));
        assertTrue(html.contains("a 1,2 km"));
        assertTrue(html.contains("Aceptar"));
        assertTrue(html.contains("Rechazar"));
        assertTrue(html.contains("https://service.test/api/v1/requests/email/accept?requestId=1&amp;token=abc"));
        assertTrue(html.contains("https://service.test/api/v1/requests/email/reject?requestId=1&amp;token=def"));
        assertTrue(html.contains("notification-in"));
        assertTrue(html.contains("4.2s"));
    }

    @Test
    void contractorHtmlContieneLaConfirmacion() {
        NotificationRequest request = new NotificationRequest(
                UUID.randomUUID(), "Creada", "Solicitud creada", "msg",
                NotificationType.REQUEST_CREATED, UUID.randomUUID(), null, null, "contratante@correo.com");
        Notification notification = new Notification();
        notification.setRequestId(request.requestId());

        String html = template.contractorHtml(request, notification);

        assertTrue(html.contains("Solicitud enviada"));
        assertTrue(html.contains("Los trabajadores disponibles ya fueron notificados."));
        assertTrue(html.contains("Referencia " + request.requestId()));
        assertTrue(html.contains("OficioYa"));
        assertTrue(html.contains("#fff0e9"));
        assertTrue(html.contains("cid:oficioya-logo"));
    }

    @Test
    void plainTextIncluyeDatosDeLaSolicitud() {
        NotificationRequest request = new NotificationRequest(
                UUID.randomUUID(), "Solicitud entrante", "Fuga", "msg",
                NotificationType.INCOMING_REQUEST, UUID.randomUUID(), null, null, "trabajador@correo.com",
                LocalDateTime.of(2026, 3, 2, 10, 30), "Laureles", "Cra. 74 # 39-25", null);
        Notification notification = new Notification();
        notification.setDescription("Fuga");
        notification.setRequestId(request.requestId());

        String text = template.plainText(request, notification);

        assertTrue(text.startsWith("OficioYa"));
        assertTrue(text.contains("Fecha: Lun, 2 de marzo"));
        assertTrue(text.contains("Laureles"));
    }
}

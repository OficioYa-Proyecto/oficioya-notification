package oficioya_notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
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
                LocalDateTime.of(2026, 3, 2, 10, 30), "Laureles", "Cra. 74 # 39-25",
                "https://images.test/fuga.jpg");
        Notification notification = new Notification();
        notification.setDescription("Fuga de agua");
        notification.setRequestId(request.requestId());

        String html = template.workerHtml(request, notification);

        assertTrue(html.contains(">Servicio entrante</div>"));
        assertTrue(html.contains("Información sobre tu solicitud en OficioYa."));
        assertTrue(!html.contains("Alerta clara."));
        assertTrue(!html.contains("Respuesta inmediata."));
        assertTrue(!html.contains("El trabajador recibe la información esencial"));
        assertTrue(html.contains(">Servicio entrante</td></tr>"));
        assertTrue(!html.contains("SOLICITUDES"));
        assertTrue(html.contains("https://images.test/fuga.jpg"));
        assertTrue(!html.contains("photo-1581244277943"));
        assertTrue(html.contains("Nueva reparación cerca de ti"));
        assertTrue(html.contains("OficioYa"));
        assertTrue(html.contains("#dc623f"));
        assertTrue(html.contains("#fffcf7"));
        assertTrue(html.contains("#e4ded4"));
        assertTrue(html.contains("cid:oficioya-logo"));
        assertTrue(html.contains("class=\"oficioya-logo-wrap\""));
        assertTrue(html.contains("background:#ffffff !important"));
        assertTrue(html.contains("border-radius:8px"));
        assertTrue(html.contains("border-radius:6px"));
        assertTrue(html.contains("Laureles"));
        assertTrue(html.contains("a 1,2 km"));
        assertTrue(html.contains("Responde desde la app de OficioYa."));
        assertTrue(html.contains("notification-in"));
        assertTrue(html.contains("4.2s"));
    }

    @Test
    void carruselDeFotosApareceEnCorreoDelTrabajadorYDelContratante() {
        NotificationRequest request = new NotificationRequest(
                UUID.randomUUID(), "Solicitud entrante", "Fuga de agua", "msg",
                NotificationType.INCOMING_REQUEST, UUID.randomUUID(), null, null, "trabajador@correo.com",
                LocalDateTime.of(2026, 3, 2, 10, 30), "Laureles", "Cra. 74 # 39-25", null,
                null, null, null, null,
                List.of("YQ==", "Yg==", "Yw=="), List.of("image/jpeg", "image/jpeg", "image/jpeg"));
        Notification notification = new Notification();
        notification.setDescription("Fuga de agua");
        notification.setRequestId(request.requestId());

        String worker = template.workerHtml(request, notification);

        assertTrue(worker.contains("oficioya-track-3"));
        assertTrue(worker.contains("@keyframes oficioya-slide-3"));
        assertTrue(worker.contains("cid:request-photo-1"));
        assertTrue(worker.contains("cid:request-photo-3"));
        assertTrue(worker.contains("Miniatura del servicio"));
        assertTrue(!worker.contains("photo-1581244277943"));

        String contractor = template.contractorHtml(request, notification);

        assertTrue(contractor.contains("oficioya-track-3"));
        assertTrue(contractor.contains("cid:request-photo-2"));
    }

    @Test
    void contractorHtmlContieneLaConfirmacion() {
        NotificationRequest request = new NotificationRequest(
                UUID.randomUUID(), "Creada", "Solicitud creada", "msg",
                NotificationType.REQUEST_CREATED, UUID.randomUUID(), null, null, "contratante@correo.com");
        Notification notification = new Notification();
        notification.setRequestId(request.requestId());

        String html = template.contractorHtml(request, notification);

        assertTrue(html.contains(">Solicitud enviada</td></tr>"));
        assertTrue(html.contains("Solicitud enviada"));
        assertTrue(!html.contains("SOLICITUDES"));
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
        assertTrue(text.contains("Servicio entrante"));
        assertTrue(text.contains("Fecha: Lun, 2 de marzo"));
        assertTrue(text.contains("Laureles"));
    }

    @Test
    void statusAceptadoIncluyeAsuntoCuerpoYHtml() {
        UUID requestId = UUID.randomUUID();
        NotificationRequest request = new NotificationRequest(
                UUID.randomUUID(), null, null, "Tu solicitud fue aceptada",
                NotificationType.ACCEPTED_REQUEST, requestId, null, null, "contratante@correo.com");
        Notification notification = new Notification();
        notification.setState(NotificationType.ACCEPTED_REQUEST);
        notification.setRequestId(requestId);
        notification.setDescription("El trabajador aceptó la reparación");

        assertEquals("Tu solicitud en OficioYa fue aceptada", template.statusSubject(request, notification));
        String text = template.statusPlain(request, notification);
        assertTrue(text.contains("Solicitud aceptada"));
        assertTrue(text.contains("El trabajador aceptó tu solicitud"));
        assertTrue(text.contains("ID de solicitud: " + requestId));

        String html = template.statusHtml(request, notification);
        assertTrue(html.contains(">Solicitud aceptada</td></tr>"));
        assertTrue(!html.contains("SOLICITUDES"));
        assertTrue(html.contains("El trabajador aceptó tu solicitud"));
        assertTrue(html.contains("Referencia " + requestId));
        assertTrue(html.contains("cid:oficioya-logo"));
    }

    @Test
    void statusCubreTodosLosTiposDeNotificacion() {
        NotificationRequest request = new NotificationRequest(
                UUID.randomUUID(), "Título previo", "Descripción", "msg",
                NotificationType.ACCEPTED_REQUEST, UUID.randomUUID(), null, null, "destino@correo.com");
        Notification notification = new Notification();
        notification.setRequestId(request.requestId());
        notification.setDescription("Detalle de la reparación");

        NotificationType[] tipos = {
                NotificationType.ACCEPTED_REQUEST,
                NotificationType.REJECTED_REQUEST,
                NotificationType.CANCELLED_BY_CONTRACTOR,
                NotificationType.CANCELLED_BY_WORKER,
                NotificationType.COMPLETED_WORK,
                NotificationType.REVIEW_REPORTED,
                NotificationType.CONFLICT_REQUEST
        };
        String[] asuntos = {
                "Tu solicitud en OficioYa fue aceptada",
                "Tu solicitud en OficioYa fue rechazada",
                "Una solicitud en OficioYa fue cancelada",
                "Un trabajador canceló tu solicitud en OficioYa",
                "El trabajo en OficioYa fue completado",
                "Reporte de reseña en OficioYa",
                "Dos trabajadores aceptaron tu solicitud en OficioYa"
        };

        for (int i = 0; i < tipos.length; i++) {
            notification.setState(tipos[i]);
            assertEquals(asuntos[i], template.statusSubject(request, notification));
            assertTrue(template.statusPlain(request, notification).contains("ID de solicitud: " + request.requestId()));
            String html = template.statusHtml(request, notification);
            assertTrue(html.contains("<html"));
            assertTrue(html.contains("filter:none !important"));
        }
    }

    @Test
    void plantillasUsanValoresPorDefectoCuandoFaltanDatos() {
        NotificationRequest request = new NotificationRequest(
                UUID.randomUUID(), null, null, null,
                NotificationType.INCOMING_REQUEST, null, null, null, null);
        Notification notification = new Notification();

        String worker = template.workerHtml(request, notification);
        assertTrue(worker.contains("photo-1581244277943"));
        assertTrue(worker.contains("Lun, 2 de marzo"));
        assertTrue(worker.contains("Nueva reparación cerca de ti"));
        assertTrue(worker.contains("Responde desde la app de OficioYa."));

        String contractor = template.contractorHtml(request, notification);
        assertTrue(contractor.contains("Referencia null"));

        String text = template.plainText(request, notification);
        assertTrue(text.contains("ID de solicitud: null"));

        NotificationRequest sinTipo = new NotificationRequest(
                UUID.randomUUID(), "Título", null, null, null, null, null, null, null);
        assertEquals("Notificación OficioYa", template.statusSubject(sinTipo, notification));
        notification.setTitle("Título de la notificación");
        assertEquals("Título de la notificación", template.statusSubject(sinTipo, notification));
        assertTrue(template.statusPlain(sinTipo, notification).contains("Notificación OficioYa"));
    }

    @Test
    void botonesDelCorreoApuntanALaAccionFirmada() {
        UUID requestId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        String accept = "https://service.test/api/v1/requests/email/accept?requestId=" + requestId + "&token=abc";
        String reject = "https://service.test/api/v1/requests/email/reject?requestId=" + requestId + "&token=xyz";
        NotificationRequest request = new NotificationRequest(
                UUID.randomUUID(), "Solicitud entrante", "Fuga de agua", "msg",
                NotificationType.INCOMING_REQUEST, requestId, null, null, "trabajador@correo.com",
                LocalDateTime.of(2026, 3, 2, 10, 30), "Laureles", "Cra. 74 # 39-25",
                "https://images.test/fuga.jpg", accept, reject);
        Notification notification = new Notification();
        notification.setDescription("Fuga de agua");
        notification.setRequestId(requestId);

        String html = template.workerHtml(request, notification);

        assertTrue(html.contains("href=\"https://service.test/api/v1/requests/email/accept?requestId="
                + requestId + "&amp;token=abc\""));
        assertTrue(html.contains("href=\"https://service.test/api/v1/requests/email/reject?requestId="
                + requestId + "&amp;token=xyz\""));
        assertTrue(html.contains(">Aceptar solicitud</a>"));
        assertTrue(html.contains(">Rechazar solicitud</a>"));
        assertTrue(html.contains("role=\"button\""));
        assertTrue(html.contains("class=\"oficioya-action-cell\""));
        assertTrue(html.contains("display:block;width:100%;max-width:100%;box-sizing:border-box;min-width:0"));
        assertTrue(html.contains("@media only screen and (max-width:480px)"));
        assertTrue(!html.contains("min-width:120px"));
        assertTrue(!html.contains("padding:10px 14px"));
        assertTrue(!html.contains("Ahora no"));
        assertTrue(!html.contains(">Ver solicitud</td>"));
    }

    @Test
    void enlacesDeAccionInsegurosNoSeRenderizan() {
        NotificationRequest request = new NotificationRequest(
                UUID.randomUUID(), "Solicitud entrante", "Fuga", "msg",
                NotificationType.INCOMING_REQUEST, UUID.randomUUID(), null, null, "trabajador@correo.com",
                null, null, null, null, "javascript:alert(1)", "javascript:alert(2)");
        Notification notification = new Notification();

        String html = template.workerHtml(request, notification);

        assertTrue(!html.contains("javascript:"));
        assertTrue(html.contains("Responde desde la app de OficioYa."));
    }

    @Test
    void plantillasSoportanTemaClaroYOscuroSinInvertirImagenes() {
        NotificationRequest request = new NotificationRequest(
                UUID.randomUUID(), "Solicitud entrante", "Fuga", "msg",
                NotificationType.INCOMING_REQUEST, UUID.randomUUID(), null, null, "trabajador@correo.com",
                LocalDateTime.of(2026, 3, 2, 10, 30), "Laureles", "Cra. 74 # 39-25",
                "https://images.test/fuga.jpg");
        Notification notification = new Notification();
        notification.setDescription("Fuga");
        notification.setRequestId(request.requestId());

        String html = template.workerHtml(request, notification);

        assertTrue(html.contains("name=\"color-scheme\" content=\"light dark\""));
        assertTrue(html.contains("name=\"supported-color-schemes\" content=\"light dark\""));
        assertTrue(html.contains("@media (prefers-color-scheme: dark)"));
        assertTrue(html.contains(".oficioya-surface{background:#1d2824 !important"));
        assertTrue(html.contains(".oficioya-text{color:#f2f5f3 !important"));
        assertTrue(html.contains(".oficioya-muted{color:#a9b7b1 !important"));
        assertTrue(html.contains("class=\"oficioya-photo\""));
        assertTrue(html.contains(".oficioya-logo-wrap{background:#ffffff !important;}"));
        assertTrue(html.contains(".oficioya-photo,.oficioya-logo,img{filter:none !important"));
        assertTrue(!html.contains("filter:invert"));
        assertTrue(!html.contains("invert(1)"));
    }

    @Test
    void correoIncluyeLocalidadYBarrioPorSeparado() {
        NotificationRequest request = new NotificationRequest(
                UUID.randomUUID(), "Solicitud entrante", "Fuga de agua", "msg",
                NotificationType.INCOMING_REQUEST, UUID.randomUUID(), null, null, "trabajador@correo.com",
                LocalDateTime.of(2026, 3, 2, 10, 30), "Medellín", "Laureles", "Cra. 74 # 39-25", null,
                null, null, null, null, null, null);
        Notification notification = new Notification();
        notification.setRequestId(request.requestId());

        String html = template.workerHtml(request, notification);
        String plain = template.plainText(request, notification);

        assertTrue(html.contains("Medellín · Laureles"));
        assertTrue(plain.contains("Ubicación: Medellín · Laureles - Cra. 74 # 39-25"));
    }

    @Test
    void conflictoIncluyeBotonParaElegirTrabajadorConRootId() {
        UUID rootId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        UUID requestId = UUID.randomUUID();
        NotificationRequest request = new NotificationRequest(
                UUID.randomUUID(), "Dos trabajadores aceptaron tu solicitud. Elige cuál continúa.",
                NotificationType.CONFLICT_REQUEST, requestId, "contratante@correo.com", rootId);
        Notification notification = new Notification();
        notification.setState(NotificationType.CONFLICT_REQUEST);
        notification.setRequestId(requestId);

        assertEquals("Dos trabajadores aceptaron tu solicitud en OficioYa",
                template.statusSubject(request, notification));
        String plain = template.statusPlain(request, notification);
        assertTrue(plain.contains("Conflicto por resolver"));
        assertTrue(plain.contains("/solicitudes?conflict=" + rootId));

        String html = template.statusHtml(request, notification);
        assertTrue(html.contains(">Elegir trabajador</a>"));
        assertTrue(html.contains("href=\"https://oficioya-front-app-egh6gjc3b0aearhq.chilecentral-01.azurewebsites.net"
                + "/solicitudes?conflict=" + rootId + "\""));
        assertTrue(html.contains("Conflicto por resolver"));
    }
}

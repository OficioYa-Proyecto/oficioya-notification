package oficioya_notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import oficioya_notification.dto.EmailRequest;
import oficioya_notification.dto.NotificationRequest;
import oficioya_notification.dto.NotificationResponse;
import oficioya_notification.entity.Notification;
import oficioya_notification.mapper.ResponseMapper;
import oficioya_notification.repository.NotificationRepository;
import oficioya_notification.model.NotificationType;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ResponseMapper responseMapper;

    @Mock
    private FcmService fcmService;

    @Mock
    private EmailNotificationService emailNotificationService;

    @Mock
    private NotificationEmailTemplate emailTemplate;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void sendNotificationPersists() {
        NotificationRequest req = new NotificationRequest(
                UUID.randomUUID(), "title", "desc", "msg", oficioya_notification.model.NotificationType.INCOMING_REQUEST,
                UUID.randomUUID(), null, null);
        Notification entity = new Notification();
        NotificationResponse resp = new NotificationResponse(UUID.randomUUID(), "title", "desc", oficioya_notification.model.NotificationType.INCOMING_REQUEST,
                req.requestId(), null, null);
        when(responseMapper.toNotification(req)).thenReturn(entity);
        when(notificationRepository.save(any(Notification.class))).thenReturn(entity);
        when(responseMapper.toResponse(entity)).thenReturn(resp);

        NotificationResponse result = notificationService.sendNotification(req);
        assertEquals(resp.id(), result.id());
    }

    @Test
    void sendNotificationREQUEST_CREATEDEnviaCorreoAlContratante() {
        UUID requestId = UUID.randomUUID();
        NotificationRequest req = new NotificationRequest(
                UUID.randomUUID(), null, null, "Tu solicitud fue creada y enviada a los trabajadores",
                NotificationType.REQUEST_CREATED, requestId, null, null, "contratante@correo.com");
        Notification entity = new Notification();
        entity.setTitle("Tu solicitud fue creada y enviada a los trabajadores");
        entity.setDescription("Tu solicitud fue creada y enviada a los trabajadores");
        entity.setRequestId(requestId);
        NotificationResponse resp = new NotificationResponse(UUID.randomUUID(), "t", "d",
                NotificationType.REQUEST_CREATED, requestId, null, null);
        when(responseMapper.toNotification(req)).thenReturn(entity);
        when(notificationRepository.save(any(Notification.class))).thenReturn(entity);
        when(responseMapper.toResponse(entity)).thenReturn(resp);
        when(emailTemplate.contractorHtml(req, entity)).thenReturn("<html>Solicitud enviada</html>");

        notificationService.sendNotification(req);

        ArgumentCaptor<EmailRequest> captor = ArgumentCaptor.forClass(EmailRequest.class);
        verify(emailNotificationService).sendHtmlEmail(captor.capture(), org.mockito.ArgumentMatchers.eq("<html>Solicitud enviada</html>"));
        EmailRequest email = captor.getValue();
        assertEquals("contratante@correo.com", email.to());
        assertEquals("Tu solicitud en OficioYa fue creada", email.subject());
        assertTrue(email.body().contains("ID de solicitud: " + requestId));
        assertEquals("REQUEST_CREATED", email.type());
    }

    @Test
    void sendNotificationINCOMING_REQUESTEnviaCorreoAlTrabajador() {
        UUID requestId = UUID.randomUUID();
        NotificationRequest req = new NotificationRequest(
                UUID.randomUUID(), "Solicitud entrante", "Fuga en Laureles", "msg",
                NotificationType.INCOMING_REQUEST, requestId, null, null, "trabajador@correo.com",
                java.time.LocalDateTime.of(2026, 3, 2, 10, 30), "Laureles", "Cra. 74 # 39-25", null);
        Notification entity = new Notification();
        entity.setTitle("Solicitud entrante");
        entity.setDescription("Fuga en Laureles");
        entity.setRequestId(requestId);
        NotificationResponse resp = new NotificationResponse(UUID.randomUUID(), "t", "d",
                NotificationType.INCOMING_REQUEST, requestId, null, null);
        when(responseMapper.toNotification(req)).thenReturn(entity);
        when(notificationRepository.save(any(Notification.class))).thenReturn(entity);
        when(responseMapper.toResponse(entity)).thenReturn(resp);
        when(emailTemplate.workerHtml(req, entity)).thenReturn("<html>Solicitud entrante</html>");
        when(emailTemplate.plainText(req, entity)).thenReturn("Solicitud entrante");

        notificationService.sendNotification(req);

        ArgumentCaptor<EmailRequest> captor = ArgumentCaptor.forClass(EmailRequest.class);
        verify(emailNotificationService).sendHtmlEmail(captor.capture(), org.mockito.ArgumentMatchers.eq("<html>Solicitud entrante</html>"));
        EmailRequest email = captor.getValue();
        assertEquals("trabajador@correo.com", email.to());
        assertEquals("Solicitud entrante en OficioYa", email.subject());
        assertEquals("INCOMING_REQUEST", email.type());
    }

    @Test
    void sendNotificationReportaFalloFcmPorCorreo() {
        UUID requestId = UUID.randomUUID();
        NotificationRequest req = new NotificationRequest(
                UUID.randomUUID(), "title", "desc", "msg", NotificationType.INCOMING_REQUEST,
                requestId, null, null, "trabajador@correo.com");
        Notification entity = new Notification();
        entity.setRequestId(requestId);
        NotificationResponse resp = new NotificationResponse(UUID.randomUUID(), "t", "d",
                NotificationType.INCOMING_REQUEST, requestId, null, null);
        when(responseMapper.toNotification(req)).thenReturn(entity);
        when(notificationRepository.save(any(Notification.class))).thenReturn(entity);
        when(responseMapper.toResponse(entity)).thenReturn(resp);
        when(fcmService.sendPush(any(), any(), any(), any(), any())).thenReturn("token FCM inválido");
        when(emailTemplate.workerHtml(req, entity)).thenReturn("<html>Solicitud entrante</html>");
        when(emailTemplate.plainText(req, entity)).thenReturn("Solicitud entrante");

        notificationService.sendNotification(req);

        ArgumentCaptor<String> detalle = ArgumentCaptor.forClass(String.class);
        verify(emailNotificationService).sendFailureReport(
                org.mockito.ArgumentMatchers.eq("INCOMING_REQUEST"),
                org.mockito.ArgumentMatchers.eq("trabajador@correo.com"),
                detalle.capture());
        assertTrue(detalle.getValue().contains("FCM: token FCM inválido"));
        verify(emailNotificationService).sendHtmlEmail(any(EmailRequest.class), org.mockito.ArgumentMatchers.eq("<html>Solicitud entrante</html>"));
    }

    @Test
    void sendNotificationConFcmFallidoIgualEnviaCorreo() {
        UUID requestId = UUID.randomUUID();
        NotificationRequest req = new NotificationRequest(
                UUID.randomUUID(), "Solicitud entrante", "Fuga en Laureles", "msg", NotificationType.INCOMING_REQUEST,
                requestId, null, null, "trabajador@correo.com");
        Notification entity = new Notification();
        entity.setRequestId(requestId);
        NotificationResponse resp = new NotificationResponse(UUID.randomUUID(), "t", "d",
                NotificationType.INCOMING_REQUEST, requestId, null, null);
        when(responseMapper.toNotification(req)).thenReturn(entity);
        when(notificationRepository.save(any(Notification.class))).thenReturn(entity);
        when(responseMapper.toResponse(entity)).thenReturn(resp);
        when(emailTemplate.workerHtml(req, entity)).thenReturn("<html>Solicitud entrante</html>");
        when(emailTemplate.plainText(req, entity)).thenReturn("Solicitud entrante");
        doThrow(new IllegalStateException("Firebase caído")).when(fcmService)
                .sendPush(any(), any(), any(), any(), any());

        notificationService.sendNotification(req);

        verify(emailNotificationService).sendHtmlEmail(any(EmailRequest.class), org.mockito.ArgumentMatchers.eq("<html>Solicitud entrante</html>"));
        ArgumentCaptor<String> detalle = ArgumentCaptor.forClass(String.class);
        verify(emailNotificationService).sendFailureReport(
                org.mockito.ArgumentMatchers.eq("INCOMING_REQUEST"),
                org.mockito.ArgumentMatchers.eq("trabajador@correo.com"),
                detalle.capture());
        assertTrue(detalle.getValue().contains("Firebase caído"));
    }

    @Test
    void sendNotificationReportaCorreoDestinatarioVacio() {
        UUID requestId = UUID.randomUUID();
        NotificationRequest req = new NotificationRequest(
                UUID.randomUUID(), "title", "desc", "msg", NotificationType.REQUEST_CREATED,
                requestId, null, null);
        Notification entity = new Notification();
        entity.setRequestId(requestId);
        NotificationResponse resp = new NotificationResponse(UUID.randomUUID(), "t", "d",
                NotificationType.REQUEST_CREATED, requestId, null, null);
        when(responseMapper.toNotification(req)).thenReturn(entity);
        when(notificationRepository.save(any(Notification.class))).thenReturn(entity);
        when(responseMapper.toResponse(entity)).thenReturn(resp);

        notificationService.sendNotification(req);

        ArgumentCaptor<String> detalle = ArgumentCaptor.forClass(String.class);
        verify(emailNotificationService).sendFailureReport(
                org.mockito.ArgumentMatchers.eq("REQUEST_CREATED"),
                org.mockito.ArgumentMatchers.eq("null"),
                detalle.capture());
        assertTrue(detalle.getValue().contains("Correo del destinatario vacío"));
    }

    @Test
    void findByIdEmpty() {
        UUID id = UUID.randomUUID();
        when(notificationRepository.findById(id)).thenReturn(Optional.empty());
        assertTrue(notificationService.findById(id).isEmpty());
    }

    @Test
    void findAllForUser() {
        UUID userId = UUID.randomUUID();
        when(notificationRepository.findByReceiverId(userId)).thenReturn(List.of());
        assertEquals(0, notificationService.findAllForUser(userId).size());
    }

    @Test
    void findByIdFound() {
        UUID id = UUID.randomUUID();
        Notification entity = new Notification();
        entity.setId(id);
        NotificationResponse resp = new NotificationResponse(id, "t", "d", NotificationType.INCOMING_REQUEST, id, null, null);
        when(notificationRepository.findById(id)).thenReturn(Optional.of(entity));
        when(responseMapper.toResponse(entity)).thenReturn(resp);
        assertTrue(notificationService.findById(id).isPresent());
    }

    @Test
    void findAllForARequest() {
        UUID requestId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        when(notificationRepository.findByRequestIdAndReceiverId(requestId, receiverId)).thenReturn(List.of());
        assertEquals(0, notificationService.findAllForARequest(requestId, receiverId).size());
    }

    @Test
    void deleteNotification() {
        UUID id = UUID.randomUUID();
        when(notificationRepository.existsById(id)).thenReturn(true);
        assertTrue(notificationService.deleteNotification(id));
    }
}
package oficioya_notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;

import jakarta.mail.BodyPart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;

import oficioya_notification.dto.EmailRequest;
import oficioya_notification.exception.EmailDeliveryException;

class EmailNotificationServiceTest {

    private void setMailSender(EmailNotificationService service, JavaMailSender sender) throws Exception {
        Field field = EmailNotificationService.class.getDeclaredField("mailSender");
        field.setAccessible(true);
        field.set(service, sender);
    }

    private void setSmtpUsername(EmailNotificationService service, String username) throws Exception {
        Field field = EmailNotificationService.class.getDeclaredField("smtpUsername");
        field.setAccessible(true);
        field.set(service, username);
    }

    private void setFallbackEmail(EmailNotificationService service, String email) throws Exception {
        Field field = EmailNotificationService.class.getDeclaredField("fallbackEmail");
        field.setAccessible(true);
        field.set(service, email);
    }

    private JavaMailSender senderWithMessages() {
        JavaMailSender sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage((Session) null));
        return sender;
    }

    private String htmlOf(MimeMessage message) throws Exception {
        Object content = message.getContent();
        if (content instanceof String text) {
            return text;
        }
        if (content instanceof MimeMultipart multipart) {
            StringBuilder html = new StringBuilder();
            collect(multipart, html);
            return html.toString();
        }
        return "";
    }

    private void collect(MimeMultipart multipart, StringBuilder html) throws Exception {
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);
            Object content = part.getContent();
            if (content instanceof String text) {
                html.append(text);
            } else if (content instanceof MimeMultipart nested) {
                collect(nested, html);
            }
        }
    }

    private void collectIds(MimeMultipart multipart, StringBuilder ids) throws Exception {
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);
            String[] header = part.getHeader("Content-ID");
            if (header != null) {
                ids.append(String.join(",", header));
            }
            if (part.getContent() instanceof MimeMultipart nested) {
                collectIds(nested, ids);
            }
        }
    }

    @Test
    void destinatarioVacioLanzaExcepcion() {
        EmailNotificationService service = new EmailNotificationService();
        EmailDeliveryException error = assertThrows(EmailDeliveryException.class,
                () -> service.sendEmail(new EmailRequest("  ", "asunto", "cuerpo", "REQUEST_CREATED")));
        assertTrue(error.getMessage().contains("Destinatario vacío"));
    }

    @Test
    void sinMailSenderLanzaExcepcion() {
        EmailNotificationService service = new EmailNotificationService();
        EmailDeliveryException error = assertThrows(EmailDeliveryException.class,
                () -> service.sendEmail(new EmailRequest("destino@example.com", "asunto", "cuerpo", "REQUEST_CREATED")));
        assertTrue(error.getMessage().contains("no configurado"));
    }

    @Test
    void enviaCorreoConValoresPorDefecto() throws Exception {
        EmailNotificationService service = new EmailNotificationService();
        JavaMailSender sender = senderWithMessages();
        setMailSender(service, sender);

        String result = service.sendEmail(new EmailRequest("destino@example.com", null, null, "REQUEST_CREATED"));

        assertEquals("Recibido: correo enviado a destino@example.com", result);
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(captor.capture());
        assertEquals("Notificación OficioYa", captor.getValue().getSubject());
        assertEquals("destino@example.com", captor.getValue().getAllRecipients()[0].toString());
        assertTrue(htmlOf(captor.getValue()).contains("<!doctype html>"));
    }

    @Test
    void enviaCorreoConAsuntoYCuerpo() throws Exception {
        EmailNotificationService service = new EmailNotificationService();
        JavaMailSender sender = senderWithMessages();
        setMailSender(service, sender);

        service.sendEmail(new EmailRequest("destino@example.com", "Asunto", "Cuerpo", "INCOMING_REQUEST"));

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(captor.capture());
        assertEquals("Asunto", captor.getValue().getSubject());
        assertTrue(htmlOf(captor.getValue()).contains("Cuerpo"));
        assertTrue(htmlOf(captor.getValue()).contains("prefers-color-scheme: dark"));
        assertTrue(htmlOf(captor.getValue()).contains("filter:none !important"));
        assertTrue(htmlOf(captor.getValue()).contains("oficioya-logo-wrap"));
        assertTrue(htmlOf(captor.getValue()).contains("background:#ffffff !important"));
        assertTrue(htmlOf(captor.getValue()).contains("border-radius:8px"));
    }

    @Test
    void enviaCorreoConRemitenteOficioYa() throws Exception {
        EmailNotificationService service = new EmailNotificationService();
        JavaMailSender sender = senderWithMessages();
        setMailSender(service, sender);
        setSmtpUsername(service, "oficioya.notificaciones@gmail.com");

        service.sendEmail(new EmailRequest("destino@example.com", "Asunto", "Cuerpo", "REQUEST_CREATED"));

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(captor.capture());
        assertEquals("OficioYa <oficioya.notificaciones@gmail.com>", captor.getValue().getFrom()[0].toString());
    }

    @Test
    void enviaCorreoHtmlConRemitenteOficioYa() throws Exception {
        EmailNotificationService service = new EmailNotificationService();
        JavaMailSender sender = senderWithMessages();
        setMailSender(service, sender);
        setSmtpUsername(service, "oficioya.notificaciones@gmail.com");

        String result = service.sendHtmlEmail(
                new EmailRequest("destino@example.com", "Asunto", "Texto plano", "INCOMING_REQUEST"),
                "<html><body>Servicio entrante</body></html>");

        assertEquals("Recibido: correo enviado a destino@example.com", result);
        verify(sender).createMimeMessage();
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(captor.capture());
        assertEquals("OficioYa <oficioya.notificaciones@gmail.com>", captor.getValue().getFrom()[0].toString());
        assertTrue(htmlOf(captor.getValue()).contains("Servicio entrante"));
    }

    @Test
    void enviaCorreoConVariasFotosIncrustadasParaElCarrusel() throws Exception {
        EmailNotificationService service = new EmailNotificationService();
        JavaMailSender sender = senderWithMessages();
        setMailSender(service, sender);

        service.sendHtmlEmailWithPhotos(
                new EmailRequest("destino@example.com", "Asunto", "Texto plano", "INCOMING_REQUEST"),
                "<html><body>cid:request-photo-1 cid:request-photo-2</body></html>",
                List.of("YQ==", "Yg=="), List.of("image/jpeg", "image/png"));

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(captor.capture());
        StringBuilder ids = new StringBuilder();
        collectIds((MimeMultipart) captor.getValue().getContent(), ids);
        assertTrue(ids.toString().contains("request-photo-1"));
        assertTrue(ids.toString().contains("request-photo-2"));
    }

    @Test
    void correoHtmlSinDestinatarioLanzaExcepcion() {
        EmailNotificationService service = new EmailNotificationService();
        assertThrows(EmailDeliveryException.class, () -> service.sendHtmlEmail(
                new EmailRequest(" ", "Asunto", "Texto", "REQUEST_CREATED"), "<html></html>"));
    }

    @Test
    void correoHtmlSinMailSenderLanzaExcepcion() {
        EmailNotificationService service = new EmailNotificationService();
        assertThrows(EmailDeliveryException.class, () -> service.sendHtmlEmail(
                new EmailRequest("destino@example.com", "Asunto", "Texto", "REQUEST_CREATED"), "<html></html>"));
    }

    @Test
    void errorDeEnvioSeReportaYLanzaExcepcion() throws Exception {
        EmailNotificationService service = new EmailNotificationService();
        JavaMailSender sender = senderWithMessages();
        doThrow(new IllegalStateException("SMTP caído")).when(sender).send(any(MimeMessage.class));
        setMailSender(service, sender);
        setFallbackEmail(service, "soporte@example.com");

        EmailDeliveryException error = assertThrows(EmailDeliveryException.class,
                () -> service.sendEmail(new EmailRequest("destino@example.com", "Asunto", "Cuerpo", "REQUEST_CREATED")));

        assertTrue(error.getMessage().contains("SMTP caído"));
        verify(sender, org.mockito.Mockito.times(2)).send(any(MimeMessage.class));
    }

    @Test
    void reporteDeFalloSeEnvíaAlCorreoDeSoporte() throws Exception {
        EmailNotificationService service = new EmailNotificationService();
        JavaMailSender sender = senderWithMessages();
        setMailSender(service, sender);
        setFallbackEmail(service, "soporte@example.com");
        setSmtpUsername(service, "oficioya.notificaciones@gmail.com");

        String result = service.sendFailureReport("FCM", "trabajador@correo.com", "token inválido");

        assertEquals("Recibido: reporte enviado a soporte@example.com", result);
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(captor.capture());
        assertEquals("soporte@example.com", captor.getValue().getAllRecipients()[0].toString());
        assertTrue(captor.getValue().getSubject().contains("FCM"));
        assertTrue(htmlOf(captor.getValue()).contains("token inválido"));
        assertTrue(htmlOf(captor.getValue()).contains("prefers-color-scheme: dark"));
    }

    @Test
    void reporteDeFalloSinCorreoDeSoporteNoSeEnvia() throws Exception {
        EmailNotificationService service = new EmailNotificationService();
        JavaMailSender sender = senderWithMessages();
        setMailSender(service, sender);

        String result = service.sendFailureReport("FCM", "trabajador@correo.com", "token inválido");

        assertTrue(result.contains("sin correo de soporte"));
        verify(sender, never()).send(any(MimeMessage.class));
    }

    @Test
    void errorDeEnvioHtmlGeneraReporteYLanzaExcepcion() throws Exception {
        EmailNotificationService service = new EmailNotificationService();
        JavaMailSender sender = senderWithMessages();
        doThrow(new IllegalStateException("SMTP caído")).when(sender).send(any(MimeMessage.class));
        setMailSender(service, sender);
        setFallbackEmail(service, "soporte@example.com");

        EmailDeliveryException error = assertThrows(EmailDeliveryException.class,
                () -> service.sendHtmlEmail(
                        new EmailRequest("destino@example.com", "Asunto", "Texto", "INCOMING_REQUEST"),
                        "<html></html>"));

        assertTrue(error.getMessage().contains("SMTP caído"));
        verify(sender, org.mockito.Mockito.times(2)).send(any(MimeMessage.class));
    }

    @Test
    void correoDeRecuperacionUsaPlantillaConCodigoYBoton() throws Exception {
        EmailNotificationService service = new EmailNotificationService();
        JavaMailSender sender = senderWithMessages();
        setMailSender(service, sender);

        service.sendEmail(new EmailRequest("destino@example.com", "Código de recuperación de contraseña - OficioYa",
                "Tu código de recuperación es: 123456 (válido por 15 minutos).", "RESET_CODE"));

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(captor.capture());
        String html = htmlOf(captor.getValue());
        assertTrue(html.contains("Restablece tu contraseña"));
        assertTrue(html.contains("123456"));
        assertTrue(html.contains("Válido por 15 minutos"));
        assertTrue(html.contains("Ir a OficioYa"));
        assertTrue(html.contains("prefers-color-scheme: dark"));
        assertTrue(html.contains("background:#ffffff !important"));
        assertTrue(html.contains("filter:none !important"));
    }

    @Test
    void avisoDeCambioDeContrasenaUsaPlantillaSegura() throws Exception {
        EmailNotificationService service = new EmailNotificationService();
        JavaMailSender sender = senderWithMessages();
        setMailSender(service, sender);

        service.sendEmail(new EmailRequest("destino@example.com", "Aviso de cambio de contraseña - OficioYa",
                "Tu contraseña en OficioYa fue actualizada exitosamente.", "PASSWORD_CHANGED"));

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(captor.capture());
        String html = htmlOf(captor.getValue());
        assertTrue(html.contains("Contraseña actualizada"));
        assertTrue(html.contains("Si no fuiste tú"));
        assertTrue(html.contains("Ir a OficioYa"));
        assertTrue(html.contains("prefers-color-scheme: dark"));
    }

    @Test
    void correoDeActivacionUsaPlantillaConCodigoYValido() throws Exception {
        EmailNotificationService service = new EmailNotificationService();
        JavaMailSender sender = senderWithMessages();
        setMailSender(service, sender);

        service.sendEmail(new EmailRequest("destino@example.com", "Código de activación de cuenta - OficioYa",
                "Tu código de activación para OficioYa es: 849201 (válido por 10 minutos).", "REGISTRATION_CODE"));

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(captor.capture());
        String html = htmlOf(captor.getValue());
        assertTrue(html.contains("background:#fffcf7;font-family:Arial,Helvetica,sans-serif"));
        assertTrue(html.contains("Tu código de activación para OficioYa es: 849201 (válido por 10 minutos)."));
        assertTrue(html.contains("cid:oficioya-logo"));
        assertFalse(html.contains("prefers-color-scheme: dark"));
        assertTrue(html.contains("background:#ffffff!important"));
    }
}

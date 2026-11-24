package oficioya_notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import oficioya_notification.dto.EmailRequest;

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

    @Test
    void destinatarioVacioNoEnvia() {
        EmailNotificationService service = new EmailNotificationService();
        assertEquals("Recibido: destinatario vacío",
                service.sendEmail(new EmailRequest("  ", "asunto", "cuerpo", "REQUEST_CREATED")));
    }

    @Test
    void sinMailSenderNoEnvia() {
        EmailNotificationService service = new EmailNotificationService();
        assertTrue(service.sendEmail(new EmailRequest("destino@example.com", "asunto", "cuerpo", "REQUEST_CREATED"))
                .contains("mail no configurado"));
    }

    @Test
    void enviaCorreoConValoresPorDefecto() throws Exception {
        EmailNotificationService service = new EmailNotificationService();
        JavaMailSender sender = mock(JavaMailSender.class);
        setMailSender(service, sender);

        String result = service.sendEmail(new EmailRequest("destino@example.com", null, null, "REQUEST_CREATED"));

        assertEquals("Recibido: correo enviado a destino@example.com", result);
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(captor.capture());
        assertEquals("Notificación OficioYa", captor.getValue().getSubject());
        assertEquals("", captor.getValue().getText());
    }

    @Test
    void enviaCorreoConAsuntoYCuerpo() throws Exception {
        EmailNotificationService service = new EmailNotificationService();
        JavaMailSender sender = mock(JavaMailSender.class);
        setMailSender(service, sender);

        service.sendEmail(new EmailRequest("destino@example.com", "Asunto", "Cuerpo", "INCOMING_REQUEST"));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(captor.capture());
        assertEquals("Asunto", captor.getValue().getSubject());
        assertEquals("Cuerpo", captor.getValue().getText());
    }

    @Test
    void enviaCorreoConRemitenteOficioYa() throws Exception {
        EmailNotificationService service = new EmailNotificationService();
        JavaMailSender sender = mock(JavaMailSender.class);
        setMailSender(service, sender);
        setSmtpUsername(service, "oficioya.notificaciones@gmail.com");

        service.sendEmail(new EmailRequest("destino@example.com", "Asunto", "Cuerpo", "REQUEST_CREATED"));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(captor.capture());
        assertEquals("OficioYa <oficioya.notificaciones@gmail.com>", captor.getValue().getFrom());
    }

    @Test
    void enviaCorreoHtmlConRemitenteOficioYa() throws Exception {
        EmailNotificationService service = new EmailNotificationService();
        JavaMailSender sender = mock(JavaMailSender.class);
        setMailSender(service, sender);
        setSmtpUsername(service, "oficioya.notificaciones@gmail.com");
        when(sender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        String result = service.sendHtmlEmail(
                new EmailRequest("destino@example.com", "Asunto", "Texto plano", "INCOMING_REQUEST"),
                "<html><body>Solicitud entrante</body></html>");

        assertEquals("Recibido: correo enviado a destino@example.com", result);
        verify(sender).createMimeMessage();
        verify(sender).send(any(MimeMessage.class));
    }

    @Test
    void correoHtmlSinDestinatarioNoSeEnvia() {
        EmailNotificationService service = new EmailNotificationService();
        assertEquals("Recibido: destinatario vacío", service.sendHtmlEmail(
                new EmailRequest(" ", "Asunto", "Texto", "REQUEST_CREATED"), "<html></html>"));
    }

    @Test
    void correoHtmlSinMailSenderNoSeEnvia() {
        EmailNotificationService service = new EmailNotificationService();
        assertTrue(service.sendHtmlEmail(
                new EmailRequest("destino@example.com", "Asunto", "Texto", "REQUEST_CREATED"), "<html></html>")
                .contains("mail no configurado"));
    }

    @Test
    void errorDeEnvioSeReportaSinLanzar() throws Exception {
        EmailNotificationService service = new EmailNotificationService();
        JavaMailSender sender = mock(JavaMailSender.class);
        doThrow(new IllegalStateException("SMTP caído")).when(sender).send(any(SimpleMailMessage.class));
        setMailSender(service, sender);

        String result = service.sendEmail(new EmailRequest("destino@example.com", "Asunto", "Cuerpo", "REQUEST_CREATED"));

        assertTrue(result.contains("error al enviar correo"));
        assertTrue(result.contains("SMTP caído"));
        verify(sender).send(any(SimpleMailMessage.class));
    }
}

package oficioya_notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import oficioya_notification.dto.EmailRequest;

@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${notification.smtp.username:}")
    private String smtpUsername;

    /** Correo al que se reportan los fallos de envío (SMTP o FCM). */
    @Value("${notification.fallback.email:}")
    private String fallbackEmail;

    public String sendEmail(EmailRequest request) {
        if (request.to() == null || request.to().isBlank()) {
            log.warn("Destinatario (to) vacío; no se envía correo.");
            return "Recibido: destinatario vacío";
        }
        if (mailSender == null) {
            log.warn("JavaMailSender no configurado; correo no enviado a {}", request.to());
            return "Recibido: mail no configurado (destinatario " + request.to() + ")";
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(request.to());
            applyFrom(message);
            message.setSubject(request.subject() != null ? request.subject() : "Notificación OficioYa");
            message.setText(request.body() != null ? request.body() : "");
            mailSender.send(message);
            log.info("Correo enviado a {} (tipo: {})", request.to(), request.type());
            return "Recibido: correo enviado a " + request.to();
        } catch (Exception e) {
            log.error("Error enviando correo a {}: {}", request.to(), e.getMessage());
            reportFailure(request, "SMTP", e);
            return "Recibido: error al enviar correo (" + e.getMessage() + ")";
        }
    }

    /**
     * Envía el correo enriquecido con una alternativa en texto plano para
     * clientes que no renderizan HTML.
     */
    public String sendHtmlEmail(EmailRequest request, String html) {
        if (request.to() == null || request.to().isBlank()) {
            log.warn("Destinatario (to) vacío; no se envía correo HTML.");
            return "Recibido: destinatario vacío";
        }
        if (mailSender == null) {
            log.warn("JavaMailSender no configurado; correo HTML no enviado a {}", request.to());
            return "Recibido: mail no configurado (destinatario " + request.to() + ")";
        }
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(request.to());
            String from = fromAddress();
            if (from != null) {
                helper.setFrom(from);
            }
            helper.setSubject(request.subject() != null ? request.subject() : "Notificación OficioYa");
            helper.setText(request.body() != null ? request.body() : "", html);
            helper.addInline("oficioya-logo", new ClassPathResource("brand/logo.png"));
            mailSender.send(mimeMessage);
            log.info("Correo HTML enviado a {} (tipo: {})", request.to(), request.type());
            return "Recibido: correo enviado a " + request.to();
        } catch (Exception e) {
            log.error("Error enviando correo HTML a {}: {}", request.to(), e.getMessage());
            reportFailure(request, "SMTP", e);
            return "Recibido: error al enviar correo (" + e.getMessage() + ")";
        }
    }

    /**
     * Envía al correo de soporte un aviso con el error de entrega. Se usa tanto
     * para fallos SMTP como para fallos FCM reportados por NotificationService.
     */
    public String sendFailureReport(String canal, String destinatario, String detalle) {
        if (mailSender == null) {
            log.warn("No se puede reportar el fallo de {} a {}: JavaMailSender no configurado.", canal, destinatario);
            return "Recibido: reporte no enviado (mail no configurado)";
        }
        if (fallbackEmail == null || fallbackEmail.isBlank()) {
            log.warn("No se puede reportar el fallo de {}: notification.fallback.email vacío.", canal);
            return "Recibido: reporte no enviado (sin correo de soporte)";
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(fallbackEmail);
            applyFrom(message);
            message.setSubject("OficioYa: fallo de envío (" + canal + ")");
            message.setText("Se produjo un error al enviar una notificación.\n\n"
                    + "Canal: " + canal + "\n"
                    + "Destinatario: " + destinatario + "\n"
                    + "Detalle: " + detalle + "\n\nOficioYa");
            mailSender.send(message);
            log.info("Reporte de fallo de {} enviado a {}", canal, fallbackEmail);
            return "Recibido: reporte enviado a " + fallbackEmail;
        } catch (Exception reportError) {
            log.error("No se pudo reportar el fallo de {} a {}: {}", canal, fallbackEmail, reportError.getMessage());
            return "Recibido: error al reportar (" + reportError.getMessage() + ")";
        }
    }

    private void reportFailure(EmailRequest request, String canal, Exception error) {
        String detalle = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
        sendFailureReport(canal + " [" + request.type() + "]", request.to(), detalle);
    }

    private void applyFrom(SimpleMailMessage message) {
        String from = fromAddress();
        if (from != null) {
            message.setFrom(from);
        }
    }

    private String fromAddress() {
        return smtpUsername == null || smtpUsername.isBlank()
                ? null
                : "OficioYa <" + smtpUsername + ">";
    }
}

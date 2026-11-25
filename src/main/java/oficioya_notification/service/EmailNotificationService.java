package oficioya_notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import oficioya_notification.dto.EmailRequest;
import oficioya_notification.exception.EmailDeliveryException;

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

    /** Envía cualquier correo con cuerpo HTML y texto plano alternativo. */
    public String sendEmail(EmailRequest request) {
        validateRecipient(request.to(), false);
        try {
            deliverHtml(request, htmlFromText(request.body()));
            log.info("Correo enviado a {} (tipo: {})", request.to(), request.type());
            return "Recibido: correo enviado a " + request.to();
        } catch (Exception e) {
            reportFailure(request, "SMTP", e);
            throw new EmailDeliveryException(
                    "No se pudo enviar correo a " + request.to() + ": " + detail(e), e);
        }
    }

    /** Envía el correo enriquecido con una alternativa en texto plano para clientes que no renderizan HTML. */
    public String sendHtmlEmail(EmailRequest request, String html) {
        validateRecipient(request.to(), true);
        try {
            deliverHtml(request, html);
            log.info("Correo HTML enviado a {} (tipo: {})", request.to(), request.type());
            return "Recibido: correo enviado a " + request.to();
        } catch (Exception e) {
            reportFailure(request, "SMTP", e);
            throw new EmailDeliveryException(
                    "No se pudo enviar correo HTML a " + request.to() + ": " + detail(e), e);
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
        String body = "Se produjo un error al enviar una notificación.\n\n"
                + "Canal: " + canal + "\n"
                + "Destinatario: " + destinatario + "\n"
                + "Detalle: " + detalle + "\n\nOficioYa";
        EmailRequest report = new EmailRequest(
                fallbackEmail, "OficioYa: fallo de envío (" + canal + ")", body, "FAILURE_REPORT");
        try {
            deliverHtml(report, htmlFromText(body));
            log.info("Reporte de fallo de {} enviado a {}", canal, fallbackEmail);
            return "Recibido: reporte enviado a " + fallbackEmail;
        } catch (Exception reportError) {
            log.error("No se pudo reportar el fallo de {} a {}: {}", canal, fallbackEmail, detail(reportError));
            return "Recibido: error al reportar (" + detail(reportError) + ")";
        }
    }

    private void validateRecipient(String to, boolean html) {
        String suffix = html ? " HTML" : "";
        if (to == null || to.isBlank()) {
            throw new EmailDeliveryException("Destinatario vacío; no se envió el correo" + suffix + ".");
        }
        if (mailSender == null) {
            throw new EmailDeliveryException(
                    "JavaMailSender no configurado; no se envió el correo" + suffix + " a " + to);
        }
    }

    private void deliverHtml(EmailRequest request, String html) throws Exception {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        if (mimeMessage == null) {
            throw new EmailDeliveryException("JavaMailSender no devolvió un mensaje MIME válido.");
        }
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
    }

    /** Envuelve el texto plano en HTML con la identidad visual de OficioYa y soporte de tema oscuro. */
    private String htmlFromText(String text) {
        String safe = escape(text == null ? "" : text).replace("\n", "<br>");
        return "<!doctype html><html lang=\"es\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<meta name=\"color-scheme\" content=\"light dark\">"
                + "<meta name=\"supported-color-schemes\" content=\"light dark\">"
                + "<style>"
                + ":root{color-scheme:light dark;}"
                + "@media (prefers-color-scheme: dark){"
                + ".oficioya-plain-body{background:#121a17 !important;color:#f2f5f3 !important;}"
                + ".oficioya-plain-text{color:#f2f5f3 !important;}"
                + ".oficioya-logo-wrap{background:#ffffff !important;}"
                + ".oficioya-logo,img{filter:none !important;-webkit-filter:none !important;}"
                + "}"
                + "</style></head>"
                + "<body class=\"oficioya-plain-body\" style=\"margin:0;padding:24px;background:#fffcf7;"
                + "font-family:Arial,Helvetica,sans-serif;color:#24312d;\">"
                + "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\"><tr>"
                + "<td class=\"oficioya-logo-wrap\" bgcolor=\"#ffffff\" "
                + "style=\"background:#ffffff !important;border-radius:8px;padding:4px;\">"
                + "<img class=\"oficioya-logo\" src=\"cid:oficioya-logo\" alt=\"OficioYa\" width=\"92\" height=\"32\" "
                + "style=\"display:block;width:92px;height:32px;object-fit:contain;border-radius:6px;\">"
                + "</td></tr></table>"
                + "<div class=\"oficioya-plain-text\" style=\"margin-top:16px;font-size:14px;line-height:22px;\">"
                + safe + "</div>"
                + "</body></html>";
    }

    private void reportFailure(EmailRequest request, String canal, Exception error) {
        String detalle = detail(error);
        log.error("Error enviando correo a {}: {}", request.to(), detalle);
        sendFailureReport(canal + " [" + request.type() + "]", request.to(), detalle);
    }

    private String detail(Exception error) {
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }

    private String fromAddress() {
        return smtpUsername == null || smtpUsername.isBlank()
                ? null
                : "OficioYa <" + smtpUsername + ">";
    }

    private String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}

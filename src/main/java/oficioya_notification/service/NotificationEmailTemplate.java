package oficioya_notification.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import oficioya_notification.dto.NotificationRequest;
import oficioya_notification.entity.Notification;
import oficioya_notification.model.NotificationType;

/**
 * Genera el HTML de los correos de solicitudes. La paleta y el logo se tomaron
 * del front desplegado: fondo crema, naranja principal, texto verde oscuro y
 * el archivo Logo.png incrustado como imagen CID.
 *
 * Funciona en tema claro y oscuro. El modo oscuro usa !important para
 * sobreescribir estilos en línea y jamás invierte las imágenes. Los botones
 * son fluidos y se apilan en pantallas estrechas para no desbordar el celular.
 */
@Component
public class NotificationEmailTemplate {

    private static final String LOGO_CID = "cid:oficioya-logo";
    private static final String DEFAULT_IMAGE =
            "https://images.unsplash.com/photo-1581244277943-fe4a9c777189?auto=format&fit=crop&w=800&q=80";
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEE, d 'de' MMMM", new Locale("es", "ES"));
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("hh:mm a", new Locale("es", "ES"));

    private static final String COLOR_BG = "#fffcf7";
    private static final String COLOR_SURFACE = "#ffffff";
    private static final String COLOR_BORDER = "#e4ded4";
    private static final String COLOR_PRIMARY = "#dc623f";
    private static final String COLOR_PRIMARY_SOFT = "#fff0e9";
    private static final String COLOR_ACCENT = "#b7462b";
    private static final String COLOR_TEXT = "#24312d";
    private static final String COLOR_MUTED = "#68736f";

    @Value("${oficioya.front.url:https://oficioya-front-app-egh6gjc3b0aearhq.chilecentral-01.azurewebsites.net}")
    private String frontUrl = "https://oficioya-front-app-egh6gjc3b0aearhq.chilecentral-01.azurewebsites.net";

    public String workerHtml(NotificationRequest request, Notification notification) {
        String dateValue = formatDate(request.scheduledDateTime());
        String timeValue = formatTime(request.scheduledDateTime());
        String location = locationLabel(request);
        String address = valueOr(request.exactAddress(), "Cra. 74 # 39-25");
        String photoBlock = hasRequestPhotos(request)
                ? carouselHtml(request)
                : legacyPhotoHtml(request);
        String description = valueOr(request.description(),
                valueOr(notification.getDescription(), "Nueva reparación cerca de ti"));

        return htmlShell(
                "<tr><td align=\"center\" style=\"padding:28px 16px 0;\">"
                + "<div class=\"oficioya-surface\" style=\"max-width:420px;margin:0 auto;background:" + COLOR_SURFACE + ";"
                + "border:1px solid " + COLOR_BORDER + ";border-radius:16px;padding:16px;"
                + "box-shadow:0 8px 30px rgba(36,49,45,.08);"
                + "animation:notification-in 900ms cubic-bezier(.22,1.28,.36,1) both;\">"
                + brandRow("ahora")
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-top:14px;\">"
                + "<tr><td align=\"left\" class=\"oficioya-text\" style=\"font-size:19px;font-weight:750;color:" + COLOR_TEXT + ";\">Servicio entrante</td>"
                + "<td align=\"right\" style=\"width:18px;\"><span style=\"display:inline-block;width:10px;height:10px;background:"
                + COLOR_PRIMARY + ";border-radius:50%;\"></span></td></tr>"
                + "<tr><td colspan=\"2\" class=\"oficioya-muted\" style=\"font-size:12px;color:" + COLOR_MUTED + ";padding-top:3px;\">"
                + "Nueva reparación cerca de ti</td></tr>"
                + "</table>"
                + "<div class=\"oficioya-muted\" style=\"margin-top:12px;font-size:12px;color:" + COLOR_MUTED + ";line-height:18px;\">"
                + escape(description) + "</div>"
                + photoBlock
                + detailGrid(dateValue, timeValue, location, address)
                + actionRow(request.acceptUrl(), request.rejectUrl())
                + "<div style=\"height:3px;background:" + COLOR_PRIMARY_SOFT + ";border-radius:3px;margin-top:14px;overflow:hidden;\">"
                + "<div style=\"height:3px;width:100%;background:" + COLOR_PRIMARY
                + ";transform-origin:left;animation:presence 4.2s linear both;\"></div></div>"
                + "</div>"
                + "</td></tr>", "Servicio entrante");
    }

    public String contractorHtml(NotificationRequest request, Notification notification) {
        String requestId = String.valueOf(notification.getRequestId() != null
                ? notification.getRequestId() : request.requestId());

        return htmlShell(
                "<tr><td align=\"center\" style=\"padding:26px 16px 34px;\">"
                + "<div class=\"oficioya-surface\" style=\"max-width:460px;margin:0 auto;background:" + COLOR_SURFACE + ";"
                + "border:1px solid " + COLOR_BORDER + ";border-radius:16px;padding:16px 18px;"
                + "box-shadow:0 8px 30px rgba(36,49,45,.08);"
                + "animation:notification-in 900ms cubic-bezier(.22,1.28,.36,1) both;\">"
                + brandRow("Confirmación")
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-top:14px;\">"
                + "<tr><td width=\"52\" valign=\"top\">"
                + "<div class=\"oficioya-primary-soft\" style=\"width:44px;height:44px;border-radius:50%;background:" + COLOR_PRIMARY_SOFT
                + ";line-height:44px;text-align:center;\">"
                + "<span style=\"display:inline-block;width:30px;height:30px;border-radius:50%;background:"
                + COLOR_PRIMARY + ";color:#ffffff;font-size:16px;line-height:30px;text-align:center;font-weight:700;\">&#10003;</span>"
                + "</div></td>"
                + "<td align=\"left\" valign=\"top\" style=\"padding-left:12px;\">"
                + "<div class=\"oficioya-text\" style=\"font-size:16px;font-weight:750;color:" + COLOR_TEXT + ";\">Solicitud enviada</div>"
                + "<div class=\"oficioya-muted\" style=\"font-size:12px;color:" + COLOR_MUTED + ";line-height:18px;margin-top:4px;\">"
                + "Los trabajadores disponibles ya fueron notificados.</div>"
                + "<div class=\"oficioya-accent\" style=\"font-size:10px;color:" + COLOR_ACCENT + ";margin-top:8px;\">Referencia "
                + escape(requestId) + "</div>"
                + "</td></tr></table>"
                + carouselHtml(request)
                + "</div>"
                + "</td></tr>", "Solicitud enviada");
    }

    /** Correo de recuperación de acceso con el OTP destacado y enlace al front. */
    public String passwordResetHtml(String body) {
        return verificationCodeHtml(
                "Recuperación de acceso", "Restablece tu contraseña",
                "Usa este código en OficioYa para continuar:",
                "Si no solicitaste este cambio, puedes ignorar este correo.",
                "Recuperación de acceso", "Te ayudamos a recuperar el acceso a tu cuenta.",
                body);
    }

    /** Aviso de contraseña cambiada con enlace al front para iniciar sesión. */
    public String passwordChangedHtml(String body) {
        String content = recoveryCard("Seguridad de tu cuenta", "Contraseña actualizada",
                "Tu contraseña en OficioYa fue actualizada exitosamente.", "",
                "Si no fuiste tú, cambia tu contraseña de inmediato.");
        return htmlShell(content, "Seguridad de tu cuenta", "Un cambio en tu cuenta OficioYa.");
    }

    /** Correo de activación con el mismo cuerpo que el código de verificación de contraseña. */
    public String registrationCodeHtml(String body) {
        return passwordResetHtml(body);
    }

    /** Tarjeta de código compartida: cambio de contraseña y activación usan exactamente el mismo diseño. */
    private String verificationCodeHtml(String label, String title, String message,
                                        String note, String shellLabel, String shellSubtitle,
                                        String body) {
        String code = firstMatch("(\\d{4,8})", body, "------");
        String minutes = firstMatch("(\\d+)\\s*minutos", body, null);
        String validity = minutes == null
                ? "Este código tiene una vigencia limitada."
                : "Válido por " + minutes + " minutos.";

        String content = recoveryCard(label, title, message,
                "<div class=\"oficioya-primary-soft\" style=\"margin-top:16px;padding:14px 8px;background:"
                        + COLOR_PRIMARY_SOFT + ";border-radius:14px;text-align:center;font-size:30px;"
                        + "font-weight:750;letter-spacing:8px;color:" + COLOR_PRIMARY + ";\">"
                        + escape(code) + "</div>"
                        + "<div class=\"oficioya-muted\" style=\"margin-top:8px;text-align:center;font-size:12px;"
                        + "color:" + COLOR_MUTED + ";\">" + escape(validity) + "</div>",
                note);
        return htmlShell(content, shellLabel, shellSubtitle);
    }

    private String recoveryCard(String label, String title, String message,
                                String extra, String note) {
        return "<tr><td align=\"center\" style=\"padding:28px 16px 34px;\">"
                + "<div class=\"oficioya-surface\" style=\"max-width:460px;margin:0 auto;background:" + COLOR_SURFACE + ";"
                + "border:1px solid " + COLOR_BORDER + ";border-radius:16px;padding:18px;"
                + "box-shadow:0 8px 30px rgba(36,49,45,.08);"
                + "animation:notification-in 900ms cubic-bezier(.22,1.28,.36,1) both;\">"
                + brandRow(label)
                + "<div class=\"oficioya-text\" style=\"margin-top:18px;font-size:19px;font-weight:750;color:"
                + COLOR_TEXT + ";\">" + escape(title) + "</div>"
                + "<div class=\"oficioya-muted\" style=\"margin-top:8px;font-size:13px;line-height:20px;color:"
                + COLOR_MUTED + ";\">" + escape(message) + "</div>"
                + extra
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"margin-top:18px;border-top:1px solid " + COLOR_BORDER + ";\">"
                + "<tr><td class=\"oficioya-action-cell\" align=\"center\" style=\"padding:14px 4px 4px;\">"
                + actionButton(frontUrl + "/auth", "Ir a OficioYa", true)
                + "</td></tr></table>"
                + "<div class=\"oficioya-muted\" style=\"margin-top:10px;font-size:11px;line-height:17px;"
                + "color:" + COLOR_MUTED + ";\">" + escape(note) + "</div>"
                + "</div></td></tr>";
    }

    private String firstMatch(String regex, String text, String fallback) {
        if (text == null || text.isBlank()) {
            return fallback;
        }
        Matcher matcher = Pattern.compile(regex).matcher(text);
        return matcher.find() ? matcher.group(1) : fallback;
    }

    public String plainText(NotificationRequest request, Notification notification) {
        String dateValue = formatDate(request.scheduledDateTime());
        String timeValue = formatTime(request.scheduledDateTime());
        String location = locationLabel(request);
        String address = valueOr(request.exactAddress(), "Cra. 74 # 39-25");
        String description = valueOr(notification.getDescription(),
                valueOr(request.description(), "Nueva reparación cerca de ti"));

        return "OficioYa\n"
                + "Servicio entrante\n"
                + description + "\n\n"
                + "Fecha: " + dateValue + "\n"
                + "Hora: " + timeValue + "\n"
                + "Ubicación: " + location + " - " + address + "\n\n"
                + "ID de solicitud: " + notification.getRequestId() + "\n";
    }

    private boolean hasRequestPhotos(NotificationRequest request) {
        return request != null && request.photoBase64s() != null && !request.photoBase64s().isEmpty();
    }

    private String legacyPhotoHtml(NotificationRequest request) {
        String image = (request.imageBase64() != null && !request.imageBase64().isBlank())
                ? "cid:request-photo"
                : valueOr(request.imageUrl(), DEFAULT_IMAGE);
        return "<img class=\"oficioya-photo\" src=\"" + escape(image) + "\" alt=\"Fotografía de la avería\" width=\"100%\" height=\"94\" "
                + "style=\"display:block;width:100%;height:94px;object-fit:cover;border-radius:14px;margin-top:12px;\">";
    }

    private List<String> requestPhotoSources(NotificationRequest request) {
        List<String> sources = new ArrayList<>();
        if (!hasRequestPhotos(request)) {
            return sources;
        }
        for (int i = 0; i < request.photoBase64s().size() && sources.size() < 5; i++) {
            String encoded = request.photoBase64s().get(i);
            if (encoded != null && !encoded.isBlank()) {
                sources.add("cid:request-photo-" + (i + 1));
            }
        }
        return sources;
    }

    /**
     * Carrusel sin JavaScript: las fotos se desplazan solas con CSS y las miniaturas
     * inferiores son el respaldo visible en clientes que no animan el contenido.
     */
    private String carouselHtml(NotificationRequest request) {
        List<String> sources = requestPhotoSources(request);
        if (sources.isEmpty()) {
            return "";
        }
        StringBuilder html = new StringBuilder();
        html.append("<div style=\"margin-top:12px;\">")
                .append("<div class=\"oficioya-carousel-window\" style=\"height:94px;\">")
                .append("<div class=\"oficioya-track oficioya-track-").append(Math.min(sources.size(), 5))
                .append("\">");
        for (String source : sources) {
            html.append("<img class=\"oficioya-photo\" src=\"").append(escape(source))
                    .append("\" alt=\"Foto del servicio\" width=\"100%\" height=\"94\" "
                            + "style=\"display:block;width:100%;height:94px;object-fit:cover;\">");
        }
        html.append("</div></div>");
        if (sources.size() > 1) {
            html.append("<div style=\"margin-top:8px;\">");
            for (String source : sources) {
                html.append("<img class=\"oficioya-photo\" src=\"").append(escape(source))
                        .append("\" alt=\"Miniatura del servicio\" width=\"56\" height=\"42\" "
                                + "style=\"display:inline-block;width:56px;height:42px;object-fit:cover;"
                                + "border-radius:8px;margin:0 6px 6px 0;border:1px solid ")
                .append(COLOR_BORDER).append(";\">");
            }
            html.append("</div>");
        }
        html.append("</div>");
        return html.toString();
    }

    /** Asunto de los cambios de estado: aceptar, rechazar, cancelar y completar. */
    public String statusSubject(NotificationRequest request, Notification notification) {
        NotificationType type = typeOf(request, notification);
        if (type == null) {
            return notification != null && notification.getTitle() != null && !notification.getTitle().isBlank()
                    ? notification.getTitle() : "Notificación OficioYa";
        }
        return switch (type) {
            case ACCEPTED_REQUEST -> "Tu solicitud en OficioYa fue aceptada";
            case REJECTED_REQUEST -> "Tu solicitud en OficioYa fue rechazada";
            case CANCELLED_BY_CONTRACTOR -> "Una solicitud en OficioYa fue cancelada";
            case CANCELLED_BY_WORKER -> "Un trabajador canceló tu solicitud en OficioYa";
            case COMPLETED_WORK -> "El trabajo en OficioYa fue completado";
            case REVIEW_REPORTED -> "Reporte de reseña en OficioYa";
            case CONFLICT_REQUEST -> "Dos trabajadores aceptaron tu solicitud en OficioYa";
            default -> notification != null && notification.getTitle() != null && !notification.getTitle().isBlank()
                    ? notification.getTitle() : "Notificación OficioYa";
        };
    }

    /** Texto plano alternativo del correo de cambio de estado. */
    public String statusPlain(NotificationRequest request, Notification notification) {
        String[] copy = statusCopy(typeOf(request, notification));
        String description = valueOr(notification != null ? notification.getDescription() : null,
                valueOr(request.description(), ""));
        String requestId = String.valueOf(notification != null && notification.getRequestId() != null
                ? notification.getRequestId() : request.requestId());
        NotificationType type = typeOf(request, notification);

        String cta = type == NotificationType.COMPLETED_WORK
                ? "\nCalifica el servicio en OficioYa: " + ratingUrl(requestId) + "\n"
                : "";

        return "OficioYa\n"
                + copy[0] + "\n\n"
                + copy[1] + "\n"
                + (description.isBlank() ? "" : description + "\n")
                + "\nID de solicitud: " + requestId + "\n"
                + cta
                + (type == NotificationType.CONFLICT_REQUEST
                        ? "Elige al trabajador: " + conflictUrl(request, notification) + "\n" : "")
                + "\nOficioYa";
    }

    /** Tarjeta HTML de los cambios de estado, con la misma paleta que las demás. */
    public String statusHtml(NotificationRequest request, Notification notification) {
        String[] copy = statusCopy(typeOf(request, notification));
        String description = valueOr(notification != null ? notification.getDescription() : null,
                valueOr(request.description(), "Revisa los detalles en OficioYa."));
        NotificationType type = typeOf(request, notification);
        String requestId = String.valueOf(notification != null && notification.getRequestId() != null
                ? notification.getRequestId() : request.requestId());
        String ratingButton = type == NotificationType.COMPLETED_WORK
                ? "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"margin-top:16px;border-top:1px solid " + COLOR_BORDER + ";\">"
                + "<tr><td class=\"oficioya-action-cell\" align=\"center\" style=\"padding:14px 4px;\">"
                + actionButton(ratingUrl(requestId), "Calificar el servicio", true)
                + "</td></tr></table>"
                : "";

        String conflictButton = type == NotificationType.CONFLICT_REQUEST
                ? "<div style=\"margin-top:14px;\">"
                + "<a href=\"" + escape(conflictUrl(request, notification)) + "\" style=\"display:inline-block;padding:10px 16px;"
                + "background:" + COLOR_PRIMARY + ";color:#ffffff;text-decoration:none;border-radius:10px;"
                + "font-weight:700;font-size:13px;\">Elegir trabajador</a></div>"
                : "";

        return htmlShell(
                "<tr><td align=\"center\" style=\"padding:26px 16px 34px;\">"
                + "<div class=\"oficioya-surface\" style=\"max-width:460px;margin:0 auto;background:" + COLOR_SURFACE + ";"
                + "border:1px solid " + COLOR_BORDER + ";border-radius:16px;padding:16px 18px;"
                + "box-shadow:0 8px 30px rgba(36,49,45,.08);"
                + "animation:notification-in 900ms cubic-bezier(.22,1.28,.36,1) both;\">"
                + brandRow("Estado de la solicitud")
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-top:14px;\">"
                + "<tr><td width=\"42\" valign=\"top\">"
                + "<div class=\"oficioya-primary-soft\" style=\"width:36px;height:36px;border-radius:50%;background:" + COLOR_PRIMARY_SOFT
                + ";line-height:36px;text-align:center;color:" + COLOR_PRIMARY
                + ";font-size:18px;font-weight:700;\">&#10003;</div></td>"
                + "<td align=\"left\" valign=\"top\" style=\"padding-left:12px;\">"
                + "<div class=\"oficioya-text\" style=\"font-size:16px;font-weight:750;color:" + COLOR_TEXT + ";\">"
                + escape(copy[0]) + "</div>"
                + "<div class=\"oficioya-muted\" style=\"font-size:12px;color:" + COLOR_MUTED + ";line-height:18px;margin-top:4px;\">"
                + escape(copy[1]) + "</div>"
                + "</td></tr></table>"
                + "<div class=\"oficioya-text\" style=\"margin-top:12px;font-size:12px;color:" + COLOR_TEXT + ";line-height:18px;\">"
                + escape(description) + "</div>"
                + "<div class=\"oficioya-accent\" style=\"font-size:10px;color:" + COLOR_ACCENT + ";margin-top:10px;\">Referencia "
                + escape(requestId) + "</div>"
                + ratingButton
                + conflictButton
                + "</div>"
                + "</td></tr>", notificationLabel(type));
    }

    private NotificationType typeOf(NotificationRequest request, Notification notification) {
        if (notification != null && notification.getState() != null) {
            return notification.getState();
        }
        return request != null ? request.type() : null;
    }

    /** El enlace de disputa usa rootId para que el front agrupe todas las respuestas. */
    private String conflictUrl(NotificationRequest request, Notification notification) {
        UUID rootId = request != null && request.rootId() != null ? request.rootId() : null;
        if (rootId == null && notification != null) {
            rootId = notification.getRequestId();
        }
        if (rootId == null && request != null) {
            rootId = request.requestId();
        }
        return frontUrl.replaceAll("/+$", "") + "/solicitudes?conflict=" + rootId;
    }

    /** Etiqueta del tipo de notificación en la cabecera del correo. */
    private String notificationLabel(NotificationType type) {
        if (type == null) {
            return "Notificación OficioYa";
        }
        return switch (type) {
            case INCOMING_REQUEST -> "Servicio entrante";
            case REQUEST_CREATED -> "Solicitud enviada";
            case ACCEPTED_REQUEST -> "Solicitud aceptada";
            case REJECTED_REQUEST -> "Solicitud rechazada";
            case CANCELLED_BY_CONTRACTOR -> "Solicitud cancelada";
            case CANCELLED_BY_WORKER -> "Solicitud cancelada por el trabajador";
            case COMPLETED_WORK -> "Trabajo completado";
            case REVIEW_REPORTED -> "Reporte de reseña";
            case CONFLICT_REQUEST -> "Conflicto por resolver";
        };
    }

    private String[] statusCopy(NotificationType type) {
        if (type == null) {
            return new String[] { "Notificación OficioYa", "Hay novedades sobre tu solicitud." };
        }
        return switch (type) {
            case ACCEPTED_REQUEST -> new String[] { "Solicitud aceptada",
                    "El trabajador aceptó tu solicitud. Puedes revisar los detalles en OficioYa." };
            case REJECTED_REQUEST -> new String[] { "Solicitud rechazada",
                    "El trabajador rechazó tu solicitud. Puedes publicar una nueva cuando quieras." };
            case CANCELLED_BY_CONTRACTOR -> new String[] { "Solicitud cancelada",
                    "El contratante canceló la solicitud. Ya no estará activa." };
            case CANCELLED_BY_WORKER -> new String[] { "Solicitud cancelada por el trabajador",
                    "El trabajador canceló la solicitud. Puedes publicar una nueva cuando quieras." };
            case COMPLETED_WORK -> new String[] { "Trabajo completado",
                    "El trabajador marcó el servicio como terminado." };
            case REVIEW_REPORTED -> new String[] { "Reseña reportada",
                    "Reporte de reseña recibido. El administrador lo revisará." };
            case CONFLICT_REQUEST -> new String[] { "Conflicto por resolver",
                    "Dos trabajadores aceptaron tu solicitud. Entra para elegir cuál continúa." };
            default -> new String[] { "Notificación OficioYa", "Hay novedades sobre tu solicitud." };
        };
    }

    /**
     * Envoltorio común: metadatos que evitan la inversión automática y paleta
     * oscura con !important. Las imágenes nunca se invierten.
     */
    private String htmlShell(String content, String notificationLabel) {
        return htmlShell(content, notificationLabel, "Información sobre tu solicitud en OficioYa.");
    }

    private String htmlShell(String content, String notificationLabel, String subtitle) {
        return "<!doctype html><html lang=\"es\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<meta name=\"color-scheme\" content=\"light dark\">"
                + "<meta name=\"supported-color-schemes\" content=\"light dark\">"
                + "<style>"
                + ":root{color-scheme:light dark;}"
                + "@keyframes notification-in{from{opacity:0;transform:translateY(-18px) scale(.94);}"
                + "to{opacity:1;transform:translateY(0) scale(1);}}"
                + "@keyframes presence{from{transform:scaleX(1);}to{transform:scaleX(0);}}"
                + "@keyframes oficioya-slide-2{0%,38%{transform:translateX(0)}50%,88%{transform:translateX(-100%)}100%{transform:translateX(0)}}"
                + "@keyframes oficioya-slide-3{0%,25%{transform:translateX(0)}33%,58%{transform:translateX(-100%)}66%,91%{transform:translateX(-200%)}100%{transform:translateX(0)}}"
                + "@keyframes oficioya-slide-4{0%,19%{transform:translateX(0)}25%,44%{transform:translateX(-100%)}50%,69%{transform:translateX(-200%)}75%,94%{transform:translateX(-300%)}100%{transform:translateX(0)}}"
                + "@keyframes oficioya-slide-5{0%,15%{transform:translateX(0)}20%,35%{transform:translateX(-100%)}40%,55%{transform:translateX(-200%)}60%,75%{transform:translateX(-300%)}80%,95%{transform:translateX(-400%)}100%{transform:translateX(0)}}"
                + ".oficioya-carousel-window{width:100%;overflow:hidden;border-radius:14px;}"
                + ".oficioya-track{display:flex;width:100%;}"
                + ".oficioya-track img{flex:0 0 100%;width:100%;height:94px;object-fit:cover;display:block;}"
                + ".oficioya-track-2{animation:oficioya-slide-2 12s ease-in-out infinite;}"
                + ".oficioya-track-3{animation:oficioya-slide-3 15s ease-in-out infinite;}"
                + ".oficioya-track-4{animation:oficioya-slide-4 18s ease-in-out infinite;}"
                + ".oficioya-track-5{animation:oficioya-slide-5 21s ease-in-out infinite;}"
                + "@media (prefers-color-scheme: dark){"
                + ".oficioya-bg{background:#121a17 !important;}"
                + ".oficioya-surface{background:#1d2824 !important;border-color:#34443e !important;}"
                + ".oficioya-border{border-color:#34443e !important;}"
                + ".oficioya-text{color:#f2f5f3 !important;}"
                + ".oficioya-muted{color:#a9b7b1 !important;}"
                + ".oficioya-primary{color:#ff8c63 !important;}"
                + ".oficioya-accent{color:#ffab8a !important;}"
                + ".oficioya-primary-soft{background:#3a261f !important;}"
                + ".oficioya-logo-wrap{background:#ffffff !important;}.oficioya-photo,.oficioya-logo,img{filter:none !important;-webkit-filter:none !important;}"
                + "}"
                + "@media only screen and (max-width:480px){"
                + ".oficioya-action-cell,u + .body .oficioya-action-cell{display:block !important;"
                + "width:100% !important;padding:6px 0 !important;}"
                + ".oficioya-action,u + .body .oficioya-action{display:block !important;width:100% !important;"
                + "max-width:100% !important;min-width:0 !important;box-sizing:border-box !important;"
                + "padding:12px 8px !important;text-align:center !important;}"
                + "}"
                + "</style></head>"
                + "<body class=\"oficioya-bg\" style=\"margin:0;padding:0;background:" + COLOR_BG
                + ";font-family:'DM Sans Variable',Arial,Helvetica,sans-serif;color:" + COLOR_TEXT + ";\">"
                + "<table role=\"presentation\" class=\"oficioya-bg\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:"
                + COLOR_BG + ";\">"
                + "<tr><td align=\"center\" style=\"padding:0 16px;\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:600px;\">"
                + "<tr><td style=\"background:" + COLOR_PRIMARY
                + ";border-radius:0 0 20px 20px;padding:20px 22px;color:#ffffff;\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">"
                + "<tr><td style=\"font-size:14px;font-weight:750;letter-spacing:-.2px;\">OficioYa</td>"
                + "<td align=\"right\" style=\"font-size:11px;letter-spacing:1px;color:rgba(255,255,255,.85);\">"
                + escape(notificationLabel) + "</td></tr>"
                + "</table>"
                + "<div style=\"font-size:20px;font-weight:750;color:#ffffff;margin-top:16px;\">"
                + escape(notificationLabel) + "</div>"
                + "<div style=\"font-size:13px;line-height:20px;color:rgba(255,255,255,.9);margin-top:8px;\">"
                + escape(subtitle) + "</div>"
                + "</td></tr>"
                + content
                + "<tr><td align=\"center\" class=\"oficioya-muted\" style=\"padding:18px 0 26px;font-size:11px;color:" + COLOR_MUTED
                + ";\">OficioYa · Notificaciones</td></tr>"
                + "</table></td></tr></table></body></html>";
    }

    private String brandRow(String rightLabel) {
        return "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" aria-label=\"OficioYa\">"
                + "<tr>"
                + "<td class=\"oficioya-logo-wrap\" width=\"100\" valign=\"middle\" bgcolor=\"#ffffff\" "
                + "style=\"background:#ffffff !important;border-radius:8px;padding:4px;\">"
                + "<img class=\"oficioya-logo\" src=\"" + LOGO_CID
                + "\" alt=\"OficioYa\" width=\"92\" height=\"32\" "
                + "style=\"display:block;width:92px;height:32px;object-fit:contain;border-radius:6px;\"></td>"
                + "<td align=\"right\" valign=\"middle\" class=\"oficioya-muted\" style=\"font-size:11px;color:" + COLOR_MUTED + ";\">"
                + escape(rightLabel) + "</td>"
                + "</tr></table>";
    }

    private String detailGrid(String dateValue, String timeValue, String location, String address) {
        return "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-top:14px;\">"
                + "<tr>"
                + "<td width=\"50%\" class=\"oficioya-border\" style=\"padding:8px 6px;border:1px solid " + COLOR_BORDER
                + ";border-radius:12px;vertical-align:top;\">"
                + "<div class=\"oficioya-primary\" style=\"font-size:11px;color:" + COLOR_PRIMARY + ";\">" + icon("calendar") + " "
                + escape(dateValue) + "</div>"
                + "<div class=\"oficioya-muted\" style=\"font-size:10px;color:" + COLOR_MUTED + ";margin-top:2px;\">Fecha</div></td>"
                + "<td width=\"50%\" class=\"oficioya-border\" style=\"padding:8px 6px;border:1px solid " + COLOR_BORDER
                + ";border-radius:12px;vertical-align:top;\">"
                + "<div class=\"oficioya-primary\" style=\"font-size:11px;color:" + COLOR_PRIMARY + ";\">" + icon("clock") + " "
                + escape(timeValue) + "</div>"
                + "<div class=\"oficioya-muted\" style=\"font-size:10px;color:" + COLOR_MUTED + ";margin-top:2px;\">Hora</div></td>"
                + "</tr>"
                + "<tr><td colspan=\"2\" class=\"oficioya-border\" style=\"padding:8px 6px;border:1px solid " + COLOR_BORDER
                + ";border-radius:12px;margin-top:8px;\">"
                + "<div class=\"oficioya-primary\" style=\"font-size:11px;color:" + COLOR_PRIMARY + ";\">" + icon("pin") + " "
                + escape(location) + "</div>"
                + "<div class=\"oficioya-muted\" style=\"font-size:10px;color:" + COLOR_MUTED + ";margin-top:2px;\">"
                + escape(address) + " · a 1,2 km</div>"
                + "</td></tr>"
                + "</table>";
    }

    /**
     * Botones reales: los enlaces están firmados por Service y ejecutan la acción
     * al abrirlos, redirigiendo después al front con el resultado.
     */
    private String actionRow(String acceptUrl, String rejectUrl) {
        String accept = safeActionUrl(acceptUrl);
        String reject = safeActionUrl(rejectUrl);
        if (accept == null && reject == null) {
            return "<div class=\"oficioya-muted\" style=\"margin-top:16px;padding-top:12px;border-top:1px solid "
                    + COLOR_BORDER + ";font-size:12px;color:" + COLOR_MUTED + ";\">"
                    + "Responde desde la app de OficioYa.</div>";
        }

        String acceptButton = accept == null ? "" : actionButton(accept, "Aceptar solicitud", true);
        String rejectButton = reject == null ? "" : actionButton(reject, "Rechazar solicitud", false);

        return "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"margin-top:16px;border-top:1px solid " + COLOR_BORDER + ";table-layout:fixed;\">"
                + "<tr>"
                + "<td class=\"oficioya-action-cell\" width=\"50%\" align=\"center\" style=\"padding:12px 4px;\">"
                + rejectButton + "</td>"
                + "<td class=\"oficioya-action-cell\" width=\"50%\" align=\"center\" style=\"padding:12px 4px;\">"
                + acceptButton + "</td>"
                + "</tr></table>";
    }

    private String actionButton(String url, String label, boolean primary) {
        String style = primary
                ? "background:" + COLOR_PRIMARY + ";border:1px solid " + COLOR_PRIMARY + ";color:#ffffff;"
                : "background:transparent;border:1px solid " + COLOR_BORDER + ";color:" + COLOR_PRIMARY + ";";
        return "<a class=\"oficioya-action" + (primary ? "" : " oficioya-border oficioya-primary") + "\" href=\""
                + escape(url) + "\" role=\"button\" style=\"display:block;width:100%;max-width:100%;"
                + "box-sizing:border-box;min-width:0;padding:10px 8px;border-radius:10px;"
                + "text-decoration:none;font-size:12px;line-height:16px;text-align:center;font-weight:700;"
                + style + "\">" + escape(label) + "</a>";
    }

    /** Enlace al front para calificar el servicio cumplido (válido para ambos roles). */
    private String ratingUrl(String requestId) {
        return frontUrl.replaceAll("/+$", "") + "/solicitudes?calificar=" + requestId;
    }

    /** Sólo se aceptan enlaces http(s); nunca un esquema javascript: o similar. */
    private String safeActionUrl(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.matches("(?i)^https?://.*") ? trimmed : null;
    }

    private String formatDate(LocalDateTime value) {
        if (value == null) {
            return "Lun, 2 de marzo";
        }
        String formatted = value.format(DATE_FORMAT);
        return formatted.substring(0, 1).toUpperCase(Locale.ROOT) + formatted.substring(1);
    }

    private String formatTime(LocalDateTime value) {
        return value == null ? "10:30 a. m." : value.format(TIME_FORMAT);
    }

    private String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String locationLabel(NotificationRequest request) {
        String locality = request.locality() == null ? "" : request.locality().trim();
        String neighborhood = valueOr(request.neighborhood(), "Laureles");
        if (locality.isEmpty() || locality.equals(neighborhood)) {
            return neighborhood;
        }
        return locality + " · " + neighborhood;
    }

    private String icon(String name) {
        String path = switch (name) {
            case "calendar" -> "<rect x=\"3\" y=\"5\" width=\"18\" height=\"16\" rx=\"2\"/><path d=\"M8 3v4M16 3v4M3 10h18\"/>";
            case "clock" -> "<circle cx=\"12\" cy=\"12\" r=\"9\"/><path d=\"M12 7v5l3 2\"/>";
            case "pin" -> "<path d=\"M12 21s7-6.2 7-11a7 7 0 1 0-14 0c0 4.8 7 11 7 11Z\"/><circle cx=\"12\" cy=\"10\" r=\"2.5\"/>";
            default -> "";
        };
        return "<svg width=\"13\" height=\"13\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" "
                + "stroke-width=\"1.8\" stroke-linecap=\"round\" stroke-linejoin=\"round\" "
                + "style=\"vertical-align:-2px;\">" + path + "</svg>";
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}

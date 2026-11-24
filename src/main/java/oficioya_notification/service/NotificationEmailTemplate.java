package oficioya_notification.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.stereotype.Component;

import oficioya_notification.dto.NotificationRequest;
import oficioya_notification.entity.Notification;

/**
 * Genera el HTML de los correos de solicitudes. La paleta y el logo se tomaron
 * del front desplegado: fondo crema, naranja principal, texto verde oscuro y
 * el archivo Logo.png incrustado como imagen CID.
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

    public String workerHtml(NotificationRequest request, Notification notification) {
        String dateValue = formatDate(request.scheduledDateTime());
        String timeValue = formatTime(request.scheduledDateTime());
        String neighborhood = valueOr(request.neighborhood(), "Laureles");
        String address = valueOr(request.exactAddress(), "Cra. 74 # 39-25");
        String image = valueOr(request.imageUrl(), DEFAULT_IMAGE);
        String description = valueOr(request.description(),
                valueOr(notification.getDescription(), "Nueva reparación cerca de ti"));

        return htmlShell(
                "<tr><td align=\"center\" style=\"padding:28px 16px 0;\">"
                + "<div style=\"max-width:420px;margin:0 auto;background:" + COLOR_SURFACE + ";"
                + "border:1px solid " + COLOR_BORDER + ";border-radius:16px;padding:16px;"
                + "box-shadow:0 8px 30px rgba(36,49,45,.08);"
                + "animation:notification-in 900ms cubic-bezier(.22,1.28,.36,1) both;\">"
                + brandRow("ahora")
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-top:14px;\">"
                + "<tr><td align=\"left\" style=\"font-size:19px;font-weight:750;color:" + COLOR_TEXT + ";\">Solicitud entrante</td>"
                + "<td align=\"right\" style=\"width:18px;\"><span style=\"display:inline-block;width:10px;height:10px;background:"
                + COLOR_PRIMARY + ";border-radius:50%;\"></span></td></tr>"
                + "<tr><td colspan=\"2\" style=\"font-size:12px;color:" + COLOR_MUTED + ";padding-top:3px;\">"
                + "Nueva reparación cerca de ti</td></tr>"
                + "</table>"
                + "<div style=\"margin-top:12px;font-size:12px;color:" + COLOR_MUTED + ";line-height:18px;\">"
                + escape(description) + "</div>"
                + "<img src=\"" + escape(image) + "\" alt=\"Fotografía de la avería\" width=\"100%\" height=\"94\" "
                + "style=\"display:block;width:100%;height:94px;object-fit:cover;border-radius:14px;margin-top:12px;\">"
                + detailGrid(dateValue, timeValue, neighborhood, address)
                + actionRow(request.acceptUrl(), request.rejectUrl())
                + "<div style=\"height:3px;background:" + COLOR_PRIMARY_SOFT + ";border-radius:3px;margin-top:14px;overflow:hidden;\">"
                + "<div style=\"height:3px;width:100%;background:" + COLOR_PRIMARY
                + ";transform-origin:left;animation:presence 4.2s linear both;\"></div></div>"
                + "</div>"
                + "</td></tr>");
    }

    public String contractorHtml(NotificationRequest request, Notification notification) {
        String requestId = String.valueOf(notification.getRequestId() != null
                ? notification.getRequestId() : request.requestId());

        return htmlShell(
                "<tr><td align=\"center\" style=\"padding:26px 16px 34px;\">"
                + "<div style=\"max-width:460px;margin:0 auto;background:" + COLOR_SURFACE + ";"
                + "border:1px solid " + COLOR_BORDER + ";border-radius:16px;padding:16px 18px;"
                + "box-shadow:0 8px 30px rgba(36,49,45,.08);"
                + "animation:notification-in 900ms cubic-bezier(.22,1.28,.36,1) both;\">"
                + brandRow("Confirmación")
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-top:14px;\">"
                + "<tr><td width=\"52\" valign=\"top\">"
                + "<div style=\"width:44px;height:44px;border-radius:50%;background:" + COLOR_PRIMARY_SOFT
                + ";line-height:44px;text-align:center;\">"
                + "<span style=\"display:inline-block;width:30px;height:30px;border-radius:50%;background:"
                + COLOR_PRIMARY + ";color:#ffffff;font-size:16px;line-height:30px;text-align:center;font-weight:700;\">&#10003;</span>"
                + "</div></td>"
                + "<td align=\"left\" valign=\"top\" style=\"padding-left:12px;\">"
                + "<div style=\"font-size:16px;font-weight:750;color:" + COLOR_TEXT + ";\">Solicitud enviada</div>"
                + "<div style=\"font-size:12px;color:" + COLOR_MUTED + ";line-height:18px;margin-top:4px;\">"
                + "Los trabajadores disponibles ya fueron notificados.</div>"
                + "<div style=\"font-size:10px;color:" + COLOR_ACCENT + ";margin-top:8px;\">Referencia "
                + escape(requestId) + "</div>"
                + "</td></tr></table>"
                + "</div>"
                + "</td></tr>");
    }

    public String plainText(NotificationRequest request, Notification notification) {
        String dateValue = formatDate(request.scheduledDateTime());
        String timeValue = formatTime(request.scheduledDateTime());
        String neighborhood = valueOr(request.neighborhood(), "Laureles");
        String address = valueOr(request.exactAddress(), "Cra. 74 # 39-25");
        String description = valueOr(notification.getDescription(),
                valueOr(request.description(), "Nueva reparación cerca de ti"));

        return "OficioYa\n"
                + "Solicitud entrante\n"
                + description + "\n\n"
                + "Fecha: " + dateValue + "\n"
                + "Hora: " + timeValue + "\n"
                + "Ubicación: " + neighborhood + " - " + address + "\n\n"
                + "ID de solicitud: " + notification.getRequestId() + "\n"
                + "Aceptar: " + valueOr(request.acceptUrl(), "(no disponible)") + "\n"
                + "Rechazar: " + valueOr(request.rejectUrl(), "(no disponible)") + "\n";
    }

    private String htmlShell(String content) {
        return "<!doctype html><html lang=\"es\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<style>"
                + "@keyframes notification-in{from{opacity:0;transform:translateY(-18px) scale(.94);}"
                + "to{opacity:1;transform:translateY(0) scale(1);}}"
                + "@keyframes presence{from{transform:scaleX(1);}to{transform:scaleX(0);}}"
                + "</style></head>"
                + "<body style=\"margin:0;padding:0;background:" + COLOR_BG
                + ";font-family:'DM Sans Variable',Arial,Helvetica,sans-serif;color:" + COLOR_TEXT + ";\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:"
                + COLOR_BG + ";\">"
                + "<tr><td align=\"center\" style=\"padding:0 16px;\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:600px;\">"
                + "<tr><td style=\"background:" + COLOR_PRIMARY
                + ";border-radius:0 0 20px 20px;padding:20px 22px;color:#ffffff;\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">"
                + "<tr><td style=\"font-size:14px;font-weight:750;letter-spacing:-.2px;\">OficioYa</td>"
                + "<td align=\"right\" style=\"font-size:11px;letter-spacing:1px;color:rgba(255,255,255,.85);\">SOLICITUDES</td></tr>"
                + "</table>"
                + "<div style=\"font-size:20px;font-weight:750;color:#ffffff;margin-top:16px;\">Alerta clara.</div>"
                + "<div style=\"font-size:20px;font-weight:750;color:#ffffff;\">Respuesta inmediata.</div>"
                + "<div style=\"font-size:13px;line-height:20px;color:rgba(255,255,255,.9);margin-top:8px;\">"
                + "El trabajador recibe la información esencial en el momento justo.</div>"
                + "</td></tr>"
                + content
                + "<tr><td align=\"center\" style=\"padding:18px 0 26px;font-size:11px;color:" + COLOR_MUTED
                + ";\">OficioYa · Notificaciones</td></tr>"
                + "</table></td></tr></table></body></html>";
    }

    private String brandRow(String rightLabel) {
        return "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" aria-label=\"OficioYa\">"
                + "<tr>"
                + "<td width=\"92\" valign=\"middle\"><img src=\"" + LOGO_CID
                + "\" alt=\"OficioYa\" width=\"92\" height=\"32\" "
                + "style=\"display:block;width:92px;height:32px;object-fit:contain;\"></td>"
                + "<td align=\"right\" valign=\"middle\" style=\"font-size:11px;color:" + COLOR_MUTED + ";\">"
                + escape(rightLabel) + "</td>"
                + "</tr></table>";
    }

    private String detailGrid(String dateValue, String timeValue, String neighborhood, String address) {
        return "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-top:14px;\">"
                + "<tr>"
                + "<td width=\"50%\" style=\"padding:8px 6px;border:1px solid " + COLOR_BORDER
                + ";border-radius:12px;vertical-align:top;\">"
                + "<div style=\"font-size:11px;color:" + COLOR_PRIMARY + ";\">" + icon("calendar") + " "
                + escape(dateValue) + "</div>"
                + "<div style=\"font-size:10px;color:" + COLOR_MUTED + ";margin-top:2px;\">Fecha</div></td>"
                + "<td width=\"50%\" style=\"padding:8px 6px;border:1px solid " + COLOR_BORDER
                + ";border-radius:12px;vertical-align:top;\">"
                + "<div style=\"font-size:11px;color:" + COLOR_PRIMARY + ";\">" + icon("clock") + " "
                + escape(timeValue) + "</div>"
                + "<div style=\"font-size:10px;color:" + COLOR_MUTED + ";margin-top:2px;\">Hora</div></td>"
                + "</tr>"
                + "<tr><td colspan=\"2\" style=\"padding:8px 6px;border:1px solid " + COLOR_BORDER
                + ";border-radius:12px;margin-top:8px;\">"
                + "<div style=\"font-size:11px;color:" + COLOR_PRIMARY + ";\">" + icon("pin") + " "
                + escape(neighborhood) + "</div>"
                + "<div style=\"font-size:10px;color:" + COLOR_MUTED + ";margin-top:2px;\">"
                + escape(address) + " · a 1,2 km</div>"
                + "</td></tr>"
                + "</table>";
    }

    private String actionRow(String acceptUrl, String rejectUrl) {
        return "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"margin-top:14px;border-top:1px solid " + COLOR_BORDER + ";table-layout:fixed;\">"
                + "<tr>"
                + actionCell(acceptUrl, "Aceptar", COLOR_PRIMARY, "#ffffff")
                + "<td width=\"1\" style=\"background:" + COLOR_BORDER + ";\"></td>"
                + actionCell(rejectUrl, "Rechazar", COLOR_SURFACE, COLOR_ACCENT)
                + "</tr></table>";
    }

    private String actionCell(String url, String label, String background, String color) {
        if (url == null || url.isBlank()) {
            return "<td width=\"50%\" align=\"center\" style=\"padding:12px 4px;font-size:12px;"
                    + "font-weight:700;color:" + COLOR_MUTED + ";\">" + escape(label) + "</td>";
        }
        return "<td width=\"50%\" align=\"center\" style=\"padding:12px 4px;\">"
                + "<a href=\"" + escape(url) + "\" style=\"display:inline-block;min-width:104px;"
                + "padding:9px 14px;border-radius:10px;background:" + background + ";color:" + color
                + ";font-size:12px;font-weight:700;text-decoration:none;\">" + escape(label) + "</a></td>";
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

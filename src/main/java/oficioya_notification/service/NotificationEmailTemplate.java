package oficioya_notification.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.stereotype.Component;

import oficioya_notification.dto.NotificationRequest;
import oficioya_notification.entity.Notification;

/**
 * Genera el HTML de los correos de solicitudes. El trabajador recibe la
 * notificación push enriquecida; el contratante recibe la confirmación visual
 * de que la solicitud ya fue enviada.
 */
@Component
public class NotificationEmailTemplate {

    private static final String DEFAULT_IMAGE =
            "https://images.unsplash.com/photo-1581244277943-fe4a9c777189?auto=format&fit=crop&w=800&q=80";
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEE, d 'de' MMMM", new Locale("es", "ES"));
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("hh:mm a", new Locale("es", "ES"));

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
                + "<div style=\"max-width:420px;margin:0 auto;background:rgba(255,255,255,.92);"
                + "border:1px solid rgba(255,255,255,.55);border-radius:26px;padding:16px;"
                + "box-shadow:0 18px 44px rgba(4,14,35,.35);backdrop-filter:blur(14px);"
                + "animation:notification-in 900ms cubic-bezier(.22,1.28,.36,1) both;\">"
                + appRow()
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-top:14px;\">"
                + "<tr><td align=\"left\" style=\"font-size:19px;font-weight:750;color:#12213a;\">Solicitud entrante</td>"
                + "<td align=\"right\" style=\"width:18px;\"><span style=\"display:inline-block;width:10px;height:10px;background:#1769e0;border-radius:50%;\"></span></td></tr>"
                + "<tr><td colspan=\"2\" style=\"font-size:12px;color:#66748a;padding-top:3px;\">Nueva reparación cerca de ti</td></tr>"
                + "</table>"
                + "<div style=\"margin-top:12px;font-size:12px;color:#52627a;line-height:18px;\">"
                + escape(description) + "</div>"
                + "<img src=\"" + escape(image) + "\" alt=\"Fotografía de la avería\" width=\"100%\" height=\"94\" "
                + "style=\"display:block;width:100%;height:94px;object-fit:cover;border-radius:14px;margin-top:12px;\">"
                + detailGrid(dateValue, timeValue, neighborhood, address)
                + actionRow()
                + "<div style=\"height:3px;background:#dce7f8;border-radius:3px;margin-top:14px;overflow:hidden;\">"
                + "<div style=\"height:3px;width:100%;background:#1769e0;transform-origin:left;animation:presence 4.2s linear both;\"></div></div>"
                + "</div>"
                + "</td></tr>");
    }

    public String contractorHtml(NotificationRequest request, Notification notification) {
        String requestId = String.valueOf(notification.getRequestId() != null
                ? notification.getRequestId() : request.requestId());

        return htmlShell(
                "<tr><td align=\"center\" style=\"padding:26px 16px 34px;\">"
                + "<div style=\"max-width:460px;margin:0 auto;background:rgba(255,255,255,.94);"
                + "border:1px solid rgba(255,255,255,.6);border-radius:18px;padding:16px 18px;"
                + "box-shadow:0 18px 40px rgba(4,14,35,.24);backdrop-filter:blur(14px);"
                + "animation:notification-in 900ms cubic-bezier(.22,1.28,.36,1) both;\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">"
                + "<tr><td width=\"44\" valign=\"top\"><div style=\"width:38px;height:38px;border-radius:50%;"
                + "background:#1769e0;color:#ffffff;font-size:20px;line-height:38px;text-align:center;"
                + "font-weight:700;\">&#10003;</div></td>"
                + "<td align=\"left\" valign=\"top\" style=\"padding-left:12px;\">"
                + "<div style=\"font-size:16px;font-weight:750;color:#12213a;\">Solicitud enviada</div>"
                + "<div style=\"font-size:12px;color:#66748a;line-height:18px;margin-top:4px;\">"
                + "Los trabajadores disponibles ya fueron notificados.</div>"
                + "<div style=\"font-size:10px;color:#8a97aa;margin-top:8px;\">Referencia "
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

        return "OFICIO YA\n"
                + "Solicitud entrante\n"
                + description + "\n\n"
                + "Fecha: " + dateValue + "\n"
                + "Hora: " + timeValue + "\n"
                + "Ubicación: " + neighborhood + " - " + address + "\n\n"
                + "ID de solicitud: " + notification.getRequestId() + "\n";
    }

    private String htmlShell(String content) {
        return "<!doctype html><html lang=\"es\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<style>"
                + "@keyframes notification-in{from{opacity:0;transform:translateY(-18px) scale(.94);}"
                + "to{opacity:1;transform:translateY(0) scale(1);}}"
                + "@keyframes presence{from{transform:scaleX(1);}to{transform:scaleX(0);}}"
                + "</style></head>"
                + "<body style=\"margin:0;padding:0;background:#eef3fb;font-family:Arial,Helvetica,sans-serif;\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#eef3fb;\">"
                + "<tr><td align=\"center\" style=\"padding:0 16px;\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:600px;\">"
                + "<tr><td style=\"background:#1769e0;border-radius:0 0 22px 22px;padding:22px;color:#ffffff;\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">"
                + "<tr><td style=\"font-size:12px;letter-spacing:2px;font-weight:700;\">03</td>"
                + "<td align=\"right\" style=\"font-size:12px;letter-spacing:1px;\">REQ-NOT-004</td></tr>"
                + "</table>"
                + "<div style=\"font-size:24px;font-weight:750;margin-top:14px;\">Alerta clara.</div>"
                + "<div style=\"font-size:24px;font-weight:750;\">Respuesta inmediata.</div>"
                + "<div style=\"font-size:13px;line-height:20px;opacity:.9;margin-top:10px;\">"
                + "El trabajador recibe la información esencial en el momento justo.</div>"
                + "</td></tr>"
                + content
                + "<tr><td align=\"center\" style=\"padding:20px 0 28px;font-size:11px;color:#8a97aa;\">OficioYa</td></tr>"
                + "</table></td></tr></table></body></html>";
    }

    private String appRow() {
        return "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">"
                + "<tr><td style=\"width:32px;\"><div style=\"width:28px;height:28px;border-radius:8px;background:#1769e0;"
                + "color:#ffffff;font-size:11px;font-weight:800;line-height:28px;text-align:center;\">OY</div></td>"
                + "<td style=\"font-size:11px;font-weight:700;letter-spacing:1px;color:#12213a;\">OFICIO YA</td>"
                + "<td align=\"right\" style=\"font-size:11px;color:#8a97aa;\">ahora</td></tr>"
                + "</table>";
    }

    private String detailGrid(String dateValue, String timeValue, String neighborhood, String address) {
        return "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-top:14px;\">"
                + "<tr>"
                + "<td width=\"50%\" style=\"padding:8px 6px;border:1px solid #e3eaf6;border-radius:12px;vertical-align:top;\">"
                + "<div style=\"font-size:11px;color:#1769e0;\">" + icon("calendar") + " " + escape(dateValue) + "</div>"
                + "<div style=\"font-size:10px;color:#8a97aa;margin-top:2px;\">Fecha</div></td>"
                + "<td width=\"50%\" style=\"padding:8px 6px;border:1px solid #e3eaf6;border-radius:12px;vertical-align:top;\">"
                + "<div style=\"font-size:11px;color:#1769e0;\">" + icon("clock") + " " + escape(timeValue) + "</div>"
                + "<div style=\"font-size:10px;color:#8a97aa;margin-top:2px;\">Hora</div></td>"
                + "</tr>"
                + "<tr><td colspan=\"2\" style=\"padding:8px 6px;border:1px solid #e3eaf6;border-radius:12px;margin-top:8px;\">"
                + "<div style=\"font-size:11px;color:#1769e0;\">" + icon("pin") + " " + escape(neighborhood) + "</div>"
                + "<div style=\"font-size:10px;color:#8a97aa;margin-top:2px;\">" + escape(address) + " · a 1,2 km</div>"
                + "</td></tr>"
                + "</table>";
    }

    private String actionRow() {
        return "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"margin-top:14px;border-top:1px solid #e3eaf6;table-layout:fixed;\">"
                + "<tr>"
                + "<td width=\"50%\" align=\"center\" style=\"padding:12px 4px;font-size:12px;color:#66748a;\">Ahora no</td>"
                + "<td width=\"1\" style=\"background:#e3eaf6;\"></td>"
                + "<td width=\"50%\" align=\"center\" style=\"padding:12px 4px;font-size:12px;font-weight:700;color:#1769e0;\">Ver solicitud</td>"
                + "</tr></table>";
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

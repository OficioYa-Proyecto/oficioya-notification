package oficioya_notification.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Se lanza cuando no es posible entregar un correo. La respuesta HTTP termina
 * en error para que el productor de la notificación sepa que el envío falló.
 */
@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class EmailDeliveryException extends RuntimeException {

    public EmailDeliveryException(String message) {
        super(message);
    }

    public EmailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}

package oficioya_notification.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import oficioya_notification.entity.Notification;
import oficioya_notification.repository.NotificationRepository;

class NotificationAccessTest {

    private final NotificationRepository repositorio = mock(NotificationRepository.class);
    private final NotificationAccess acceso = new NotificationAccess(repositorio);

    private static Authentication autenticadoComo(UUID id) {
        return new UsernamePasswordAuthenticationToken(id.toString(), null, List.of());
    }

    private static Notification notificacionDe(UUID dueno) {
        Notification n = mock(Notification.class);
        when(n.getReceiverId()).thenReturn(dueno);
        return n;
    }

    @Test
    void isSelf_mismoUsuario_true() {
        UUID yo = UUID.randomUUID();
        assertTrue(acceso.isSelf(yo, autenticadoComo(yo)));
    }

    @Test
    void isSelf_otroUsuario_false() {
        assertFalse(acceso.isSelf(UUID.randomUUID(), autenticadoComo(UUID.randomUUID())));
    }

    @Test
    void isSelf_sinAutenticacion_false() {
        assertFalse(acceso.isSelf(UUID.randomUUID(), null));
    }

    @Test
    void isSelf_noAutenticado_false() {
        UUID yo = UUID.randomUUID();
        Authentication sinAutenticar = new UsernamePasswordAuthenticationToken(yo.toString(), null);
        assertFalse(acceso.isSelf(yo, sinAutenticar));
    }

    @Test
    void isSelf_idNulo_false() {
        assertFalse(acceso.isSelf(null, autenticadoComo(UUID.randomUUID())));
    }

    @Test
    void ownsNotification_esElDueno_true() {
        UUID yo = UUID.randomUUID();
        UUID notificacionId = UUID.randomUUID();
        Notification mia = notificacionDe(yo);
        when(repositorio.findById(notificacionId)).thenReturn(Optional.of(mia));
        assertTrue(acceso.ownsNotification(notificacionId, autenticadoComo(yo)));
    }

    @Test
    void ownsNotification_esDeOtro_false() {
        UUID notificacionId = UUID.randomUUID();
        Notification ajena = notificacionDe(UUID.randomUUID());
        when(repositorio.findById(notificacionId)).thenReturn(Optional.of(ajena));
        assertFalse(acceso.ownsNotification(notificacionId, autenticadoComo(UUID.randomUUID())));
    }

    @Test
    void ownsNotification_noExiste_false() {
        UUID notificacionId = UUID.randomUUID();
        when(repositorio.findById(notificacionId)).thenReturn(Optional.empty());
        assertFalse(acceso.ownsNotification(notificacionId, autenticadoComo(UUID.randomUUID())));
    }

    @Test
    void ownsNotification_idNulo_false() {
        assertFalse(acceso.ownsNotification(null, autenticadoComo(UUID.randomUUID())));
    }
}
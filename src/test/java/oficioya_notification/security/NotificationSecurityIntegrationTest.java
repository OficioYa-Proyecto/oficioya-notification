package oficioya_notification.security;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.Filter;
import oficioya_notification.controller.NotificationController;
import oficioya_notification.dto.NotificationResponse;
import oficioya_notification.entity.Notification;
import oficioya_notification.repository.NotificationRepository;
import oficioya_notification.service.EmailNotificationService;
import oficioya_notification.service.NotificationService;
import oficioya_notification.service.ReviewReportNotificationService;

/**
 * Prueba de la seguridad real (filtro + reglas de acceso) con un contexto
 * minimo.
 * Los secretos de este archivo son falsos y solo existen para pruebas.
 */
@SpringJUnitWebConfig(classes = NotificationSecurityIntegrationTest.TestConfig.class)
@TestPropertySource(properties = "app.jwt.secret=Y2xhdmUtc29sby1wYXJhLXBydWViYXMtbm90aWZpY2F0aW9uLTAxMjM0NTY3ODk=")
class NotificationSecurityIntegrationTest {

    private static final String SECRETO_PRUEBA = "Y2xhdmUtc29sby1wYXJhLXBydWViYXMtbm90aWZpY2F0aW9uLTAxMjM0NTY3ODk=";
    private static final String SECRETO_AJENO = "b3RyYS1jbGF2ZS1kaXN0aW50YS1wYXJhLWZpcm1hci10b2tlbnMtYWplbm9zLTk5OTk=";

    private static final String EMAIL_JSON = "{\"to\":\"destino@example.com\",\"subject\":\"Asunto\",\"body\":\"Cuerpo\",\"type\":\"OTP\"}";

    @Configuration
    @EnableWebMvc
    @Import({ SecurityConfig.class, NotificationController.class, NotificationAccess.class })
    static class TestConfig {
        @Bean
        NotificationService notificationService() {
            return mock(NotificationService.class);
        }

        @Bean
        ReviewReportNotificationService reviewReportNotificationService() {
            return mock(ReviewReportNotificationService.class);
        }

        @Bean
        EmailNotificationService emailNotificationService() {
            return mock(EmailNotificationService.class);
        }

        @Bean
        NotificationRepository notificationRepository() {
            return mock(NotificationRepository.class);
        }
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private NotificationService servicio;

    @Autowired
    private NotificationRepository repositorio;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(servicio, repositorio);
        Filter cadenaDeSeguridad = context.getBean("springSecurityFilterChain", Filter.class);
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(cadenaDeSeguridad).build();
    }

    private static String token(String secretoBase64, String subject, long segundosDeVigencia, String... roles) {
        SecretKey clave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretoBase64));
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .claim("roles", List.of(roles))
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plusSeconds(segundosDeVigencia)))
                .signWith(clave)
                .compact();
    }

    private static String bearer(String subject, String... roles) {
        return "Bearer " + token(SECRETO_PRUEBA, subject, 300, roles);
    }

    private static Notification notificacionDe(UUID dueno) {
        Notification n = mock(Notification.class);
        when(n.getReceiverId()).thenReturn(dueno);
        return n;
    }

    private static NotificationResponse respuesta(UUID id) {
        return new NotificationResponse(id, "titulo", "descripcion", null, null, null, null);
    }

    // ---------- Sin credenciales: 401 ----------

    @Test
    void sinToken_leerNotificacionesDeUsuario_401() throws Exception {
        mockMvc.perform(get("/api/v1/notification/user/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sinToken_enviarCorreo_401() throws Exception {
        mockMvc.perform(post("/api/v1/notification/email")
                .contentType(MediaType.APPLICATION_JSON).content(EMAIL_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sinToken_crearNotificacion_401() throws Exception {
        mockMvc.perform(post("/api/v1/notification")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sinToken_borrarNotificacion_401() throws Exception {
        mockMvc.perform(delete("/api/v1/notification/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenConFirmaAjena_401() throws Exception {
        String ajeno = "Bearer " + token(SECRETO_AJENO, UUID.randomUUID().toString(), 300, "ADMIN");
        mockMvc.perform(get("/api/v1/notification/user/{id}", UUID.randomUUID())
                .header("Authorization", ajeno))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenVencido_401() throws Exception {
        String vencido = "Bearer " + token(SECRETO_PRUEBA, UUID.randomUUID().toString(), -60, "ADMIN");
        mockMvc.perform(get("/api/v1/notification/user/{id}", UUID.randomUUID())
                .header("Authorization", vencido))
                .andExpect(status().isUnauthorized());
    }

    // ---------- Rol insuficiente o no es el dueno: 403 ----------

    @Test
    void usuarioNormal_noPuedeEnviarCorreo_403() throws Exception {
        mockMvc.perform(post("/api/v1/notification/email")
                .header("Authorization", bearer(UUID.randomUUID().toString(), "CONTRATANTE"))
                .contentType(MediaType.APPLICATION_JSON).content(EMAIL_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuarioNormal_noPuedeBorrar_403() throws Exception {
        mockMvc.perform(delete("/api/v1/notification/{id}", UUID.randomUUID())
                .header("Authorization", bearer(UUID.randomUUID().toString(), "TRABAJADOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuarioNormal_noLeeNotificacionesDeOtroUsuario_403() throws Exception {
        mockMvc.perform(get("/api/v1/notification/user/{id}", UUID.randomUUID())
                .header("Authorization", bearer(UUID.randomUUID().toString(), "CONTRATANTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void rolDesconocido_noPuedeEnviarCorreo_403() throws Exception {
        mockMvc.perform(post("/api/v1/notification/email")
                .header("Authorization", bearer(UUID.randomUUID().toString(), "HACKER"))
                .contentType(MediaType.APPLICATION_JSON).content(EMAIL_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuario_noLeeNotificacionAjenaPorId_403() throws Exception {
        UUID yo = UUID.randomUUID();
        UUID notificacionId = UUID.randomUUID();
        Notification ajena = notificacionDe(UUID.randomUUID());
        when(repositorio.findById(notificacionId)).thenReturn(Optional.of(ajena));
        when(servicio.findById(notificacionId)).thenReturn(Optional.of(respuesta(notificacionId)));

        mockMvc.perform(get("/api/v1/notification/{id}", notificacionId)
                .header("Authorization", bearer(yo.toString(), "CONTRATANTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuario_notificacionInexistenteNoRevelaNada_403() throws Exception {
        mockMvc.perform(get("/api/v1/notification/{id}", UUID.randomUUID())
                .header("Authorization", bearer(UUID.randomUUID().toString(), "TRABAJADOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuario_noLeeNotificacionesDeOtroPorSolicitud_403() throws Exception {
        mockMvc.perform(get("/api/v1/notification/user/{userId}/request/{requestId}",
                UUID.randomUUID(), UUID.randomUUID())
                .header("Authorization", bearer(UUID.randomUUID().toString(), "CONTRATANTE")))
                .andExpect(status().isForbidden());
    }

    // ---------- Accesos legitimos: 200 (deben seguir funcionando) ----------

    @Test
    void servicio_puedeEnviarCorreo_200() throws Exception {
        mockMvc.perform(post("/api/v1/notification/email")
                .header("Authorization", bearer("00000000-0000-0000-0000-000000000001", "SERVICE", "SISTEMA", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content(EMAIL_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void usuario_leeSusPropiasNotificaciones_200() throws Exception {
        UUID yo = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/notification/user/{id}", yo)
                .header("Authorization", bearer(yo.toString(), "CONTRATANTE")))
                .andExpect(status().isOk());
    }

    @Test
    void admin_leeNotificacionesDeCualquiera_200() throws Exception {
        mockMvc.perform(get("/api/v1/notification/user/{id}", UUID.randomUUID())
                .header("Authorization", bearer(UUID.randomUUID().toString(), "ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void usuario_leeSuNotificacionPorId_200() throws Exception {
        UUID yo = UUID.randomUUID();
        UUID notificacionId = UUID.randomUUID();
        Notification mia = notificacionDe(yo);
        when(repositorio.findById(notificacionId)).thenReturn(Optional.of(mia));
        when(servicio.findById(notificacionId)).thenReturn(Optional.of(respuesta(notificacionId)));

        mockMvc.perform(get("/api/v1/notification/{id}", notificacionId)
                .header("Authorization", bearer(yo.toString(), "TRABAJADOR")))
                .andExpect(status().isOk());
    }

    @Test
    void admin_leeCualquierNotificacionPorId_200() throws Exception {
        UUID notificacionId = UUID.randomUUID();
        when(servicio.findById(notificacionId)).thenReturn(Optional.of(respuesta(notificacionId)));

        mockMvc.perform(get("/api/v1/notification/{id}", notificacionId)
                .header("Authorization", bearer(UUID.randomUUID().toString(), "ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void usuario_leeSusNotificacionesDeUnaSolicitud_200() throws Exception {
        UUID yo = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/notification/user/{userId}/request/{requestId}", yo, UUID.randomUUID())
                .header("Authorization", bearer(yo.toString(), "CONTRATANTE")))
                .andExpect(status().isOk());
    }
}
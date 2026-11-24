package oficioya_notification.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/** Secretos falsos, solo para pruebas. */
class NotificationJwtFilterTest {

    private static final String SECRETO =
            "Y2xhdmUtc29sby1wYXJhLXBydWViYXMtbm90aWZpY2F0aW9uLTAxMjM0NTY3ODk=";
    private static final String SECRETO_AJENO =
            "b3RyYS1jbGF2ZS1kaXN0aW50YS1wYXJhLWZpcm1hci10b2tlbnMtYWplbm9zLTk5OTk=";

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private static String token(String secretoBase64, String sujeto, String nombreClaim, Object valorClaim) {
        SecretKey clave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretoBase64));
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(sujeto)
                .claim(nombreClaim, valorClaim)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plusSeconds(300)))
                .signWith(clave)
                .compact();
    }

    private static Authentication filtrar(NotificationJwtFilter filtro, String cabecera) throws Exception {
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        if (cabecera != null) {
            peticion.addHeader("Authorization", cabecera);
        }
        MockFilterChain cadena = new MockFilterChain();
        filtro.doFilter(peticion, new MockHttpServletResponse(), cadena);
        assertNotNull(cadena.getRequest(), "la cadena debe continuar siempre");
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private static List<String> roles(Authentication a) {
        return a.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    }

    @Test
    void tokenValido_cargaUsuarioYRoles() throws Exception {
        String t = token(SECRETO, "usuario-1", "roles", List.of("ADMIN", "SERVICE"));
        Authentication a = filtrar(new NotificationJwtFilter(SECRETO), "Bearer " + t);
        assertNotNull(a);
        assertEquals("usuario-1", a.getName());
        assertEquals(List.of("ROLE_ADMIN", "ROLE_SERVICE"), roles(a));
    }

    @Test
    void rolesDesconocidosSeDescartan() throws Exception {
        String t = token(SECRETO, "usuario-1", "roles", List.of("ADMIN", "HACKER", " trabajador "));
        Authentication a = filtrar(new NotificationJwtFilter(SECRETO), "Bearer " + t);
        assertEquals(List.of("ROLE_ADMIN", "ROLE_TRABAJADOR"), roles(a));
    }

    @Test
    void claimRoleEnTexto_seAcepta() throws Exception {
        String t = token(SECRETO, "usuario-1", "role", "CONTRATANTE");
        Authentication a = filtrar(new NotificationJwtFilter(SECRETO), "Bearer " + t);
        assertEquals(List.of("ROLE_CONTRATANTE"), roles(a));
    }

    @Test
    void tokenSinSujeto_noAutentica() throws Exception {
        String t = token(SECRETO, null, "roles", List.of("ADMIN"));
        assertNull(filtrar(new NotificationJwtFilter(SECRETO), "Bearer " + t));
    }

    @Test
    void tokenConFirmaAjena_noAutentica() throws Exception {
        String t = token(SECRETO_AJENO, "usuario-1", "roles", List.of("ADMIN"));
        assertNull(filtrar(new NotificationJwtFilter(SECRETO), "Bearer " + t));
    }

    @Test
    void tokenBasura_noAutentica_yLaCadenaContinua() throws Exception {
        assertNull(filtrar(new NotificationJwtFilter(SECRETO), "Bearer esto-no-es-un-jwt"));
    }

    @Test
    void sinCabecera_noAutentica() throws Exception {
        assertNull(filtrar(new NotificationJwtFilter(SECRETO), null));
    }

    @Test
    void secretoVacioONulo_rechazaTodo() throws Exception {
        String t = token(SECRETO, "usuario-1", "roles", List.of("ADMIN"));
        assertNull(filtrar(new NotificationJwtFilter(""), "Bearer " + t));
        assertNull(filtrar(new NotificationJwtFilter(null), "Bearer " + t));
    }

    @Test
    void secretoInvalido_rechazaTodo() throws Exception {
        String t = token(SECRETO, "usuario-1", "roles", List.of("ADMIN"));
        assertNull(filtrar(new NotificationJwtFilter("%%%no-es-base64%%%"), "Bearer " + t));
        assertNull(filtrar(new NotificationJwtFilter("AAAA"), "Bearer " + t));
    }
}
package oficioya_notification.security;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Valida el JWT (firma y vigencia) y carga usuario y roles en el contexto de seguridad.
 * Si el secreto no esta configurado, ningun token es valido (falla cerrado).
 * Nunca registra el token.
 */
public class NotificationJwtFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(NotificationJwtFilter.class);
    private static final Set<String> ROLES_PERMITIDOS =
            Set.of("CONTRATANTE", "TRABAJADOR", "ADMIN", "SERVICE", "SISTEMA");

    private final SecretKey clave;

    public NotificationJwtFilter(String secretoBase64) {
        this.clave = construirClave(secretoBase64);
        if (this.clave == null) {
            log.warn("app.jwt.secret no esta configurado o no es valido: se rechazaran todos los tokens");
        }
    }

    private static SecretKey construirClave(String secretoBase64) {
        if (secretoBase64 == null || secretoBase64.isBlank()) {
            return null;
        }
        try {
            return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretoBase64.trim()));
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String cabecera = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (clave != null && cabecera != null && cabecera.startsWith("Bearer ")) {
            try {
                Claims claims = Jwts.parser().verifyWith(clave).build()
                        .parseSignedClaims(cabecera.substring(7).trim()).getPayload();
                String sujeto = claims.getSubject();
                if (sujeto != null && !sujeto.isBlank()) {
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(sujeto, null, autoridades(claims)));
                }
            } catch (JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext();
                log.debug("Token rechazado: {}", e.getClass().getSimpleName());
            }
        }
        chain.doFilter(request, response);
    }

    private static List<GrantedAuthority> autoridades(Claims claims) {
        Object bruto = claims.get("roles");
        if (bruto == null) {
            bruto = claims.get("role");
        }
        List<?> lista = bruto instanceof List<?> l ? l : (bruto == null ? List.of() : List.of(bruto));
        return lista.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(r -> r.trim().toUpperCase())
                .filter(ROLES_PERMITIDOS::contains)
                .<GrantedAuthority>map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .toList();
    }
}

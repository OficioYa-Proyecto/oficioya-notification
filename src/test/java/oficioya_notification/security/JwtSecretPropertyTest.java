package oficioya_notification.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * En Azure el App Service corre con el perfil "prod" y entrega el secreto en la variable
 * JWT_SECRET. Esta prueba carga el application.properties base (el de todos los perfiles)
 * y comprueba que app.jwt.secret recibe ese valor. El valor de prueba es falso.
 */
@SpringJUnitConfig(JwtSecretPropertyTest.Config.class)
@TestPropertySource(properties = "JWT_SECRET=valor-falso-solo-para-pruebas")
class JwtSecretPropertyTest {

    @Configuration
    @PropertySource("classpath:application.properties")
    static class Config {
    }

    @Autowired
    private Environment environment;

    @Test
    void appJwtSecretSeTomaDeLaVariableJwtSecret() {
        assertEquals("valor-falso-solo-para-pruebas", environment.getProperty("app.jwt.secret"));
    }
}

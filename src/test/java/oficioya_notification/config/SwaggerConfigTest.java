package oficioya_notification.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class SwaggerConfigTest {

    @Test
    void exponeMetadatosDeLaApi() {
        assertNotNull(new SwaggerConfig().customOpenAPI().getInfo());
        assertEquals("1.0.0", new SwaggerConfig().customOpenAPI().getInfo().getVersion());
    }
}

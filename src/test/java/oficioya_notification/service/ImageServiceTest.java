package oficioya_notification.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ImageServiceTest {

    @Test
    void instantiate() {
        ImageService service = new ImageService();
        assertNotNull(service);
    }
}
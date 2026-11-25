package oficioya_notification.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ImageControllerTest {

    @Test
    void instantiate() {
        ImageController controller = new ImageController();
        assertNotNull(controller);
    }
}
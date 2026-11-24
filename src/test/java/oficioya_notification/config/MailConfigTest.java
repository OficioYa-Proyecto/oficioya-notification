package oficioya_notification.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

class MailConfigTest {

    @Test
    void construyeRemitenteConCredencialesInyectadas() {
        JavaMailSenderImpl sender = (JavaMailSenderImpl) new MailConfig()
                .notificationMailSender("smtp.example.com", 2525, "user@example.com", "secret");

        assertEquals("smtp.example.com", sender.getHost());
        assertEquals(2525, sender.getPort());
        assertEquals("user@example.com", sender.getUsername());
        assertEquals("secret", sender.getPassword());
        assertEquals("true", sender.getJavaMailProperties().getProperty("mail.smtp.auth"));
        assertTrue(sender.getJavaMailProperties().containsKey("mail.smtp.starttls.enable"));
    }
}

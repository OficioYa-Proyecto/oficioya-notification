package oficioya_notification.config;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * Configura el envío SMTP con claves propias para que los valores inyectados
 * en el build no sean sobrescritos por App Settings heredadas de Azure.
 */
@Configuration
public class MailConfig {

    @Bean
    @Primary
    public JavaMailSender notificationMailSender(
            @Value("${notification.smtp.host:smtp.gmail.com}") String host,
            @Value("${notification.smtp.port:587}") int port,
            @Value("${notification.smtp.username:}") String username,
            @Value("${notification.smtp.password:}") String password) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);
        sender.setDefaultEncoding("UTF-8");

        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        return sender;
    }
}

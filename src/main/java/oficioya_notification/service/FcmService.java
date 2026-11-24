package oficioya_notification.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

import jakarta.annotation.PostConstruct;

@Service
public class FcmService {

    private static final Logger log = LoggerFactory.getLogger(FcmService.class);

    @Value("${firebase.credentials.path:}")
    private String credentialsPath;

    @Value("${firebase.project.id:}")
    private String projectId;

    private boolean initialized = false;

    @PostConstruct
    public void init() {
        if (credentialsPath == null || credentialsPath.isBlank()) {
            log.warn("Firebase credentials path not configured; FCM sending disabled.");
            return;
        }
        try (java.io.InputStream serviceAccount = openCredentialsStream(credentialsPath)) {
            if (serviceAccount == null) {
                log.error("Firebase credentials not found at: {}", credentialsPath);
                return;
            }
            FirebaseOptions.Builder builder = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount));
            if (projectId != null && !projectId.isBlank()) {
                builder.setProjectId(projectId);
            }
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(builder.build());
            }
            initialized = true;
            log.info("Firebase Admin initialized for FCM (project: {}).", projectId);
        } catch (IOException e) {
            log.error("Failed to initialize Firebase Admin: {}", e.getMessage());
        }
    }

    private java.io.InputStream openCredentialsStream(String path) throws IOException {
        if (path.startsWith("classpath:")) {
            String resource = path.substring("classpath:".length());
            if (resource.startsWith("/")) {
                resource = resource.substring(1);
            }
            return getClass().getClassLoader().getResourceAsStream(resource);
        }
        return new java.io.FileInputStream(path);
    }

    public void sendPush(String deviceToken, String topic, String title, String body, Map<String, String> data) {
        if (!initialized) {
            log.warn("FCM not initialized; skipping push notification.");
            return;
        }
        try {
            Message.Builder builder = Message.builder();
            if (deviceToken != null && !deviceToken.isBlank()) {
                builder.setToken(deviceToken);
            } else if (topic != null && !topic.isBlank()) {
                builder.setTopic(topic);
            } else {
                log.warn("No FCM target (token/topic); skipping push.");
                return;
            }
            builder.setNotification(Notification.builder().setTitle(title).setBody(body).build());
            if (data != null) {
                builder.putAllData(data);
            }
            String response = FirebaseMessaging.getInstance().send(builder.build());
            log.info("FCM message sent: {}", response);
        } catch (Exception e) {
            log.error("Failed to send FCM message: {}", e.getMessage());
        }
    }
}

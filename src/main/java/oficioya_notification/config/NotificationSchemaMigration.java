package oficioya_notification.config;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * La columna type se creó originalmente como ENUM de H2 con una lista fija de valores.
 * Al agregar REQUEST_CREATED, las bases ya existentes rechazan el inserto. Esta
 * migración la convierte a VARCHAR sin borrar datos y es idempotente.
 */
@Component
public class NotificationSchemaMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(NotificationSchemaMigration.class);

    private final DataSource dataSource;

    public NotificationSchemaMigration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        try {
            String sqlType = jdbc.queryForObject(
                    "SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS "
                            + "WHERE UPPER(TABLE_NAME) = 'NOTIFICATIONS' AND UPPER(COLUMN_NAME) = 'TYPE'",
                    String.class);
            if (sqlType == null) {
                log.warn("No se encontró la columna NOTIFICATIONS.TYPE; se omite la migración.");
                return;
            }
            if ("CHARACTER VARYING".equalsIgnoreCase(sqlType) || "VARCHAR".equalsIgnoreCase(sqlType)) {
                return;
            }
            jdbc.execute("ALTER TABLE notifications ALTER COLUMN type VARCHAR(255)");
            log.info("Columna NOTIFICATIONS.TYPE convertida de {} a VARCHAR(255).", sqlType);
        } catch (Exception e) {
            log.warn("No se pudo migrar NOTIFICATIONS.TYPE: {}", e.getMessage());
        }
    }
}

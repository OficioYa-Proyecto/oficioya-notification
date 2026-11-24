package oficioya_notification.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class NotificationSchemaMigrationTest {

    @Test
    void convierteElEnumAVarcharYPermiteElNuevoValor() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:migrationtest;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("create table notifications (id uuid primary key, type enum('INCOMING_REQUEST','ACCEPTED_REQUEST'))");

        new NotificationSchemaMigration(dataSource).run(null);

        String sqlType = jdbc.queryForObject(
                "SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS "
                        + "WHERE UPPER(TABLE_NAME) = 'NOTIFICATIONS' AND UPPER(COLUMN_NAME) = 'TYPE'",
                String.class);
        assertEquals("CHARACTER VARYING", sqlType);
        jdbc.update("insert into notifications (id, type) values (?, ?)", UUID.randomUUID(), "REQUEST_CREATED");
        assertEquals(1, jdbc.queryForObject("select count(*) from notifications", Integer.class));
    }
}

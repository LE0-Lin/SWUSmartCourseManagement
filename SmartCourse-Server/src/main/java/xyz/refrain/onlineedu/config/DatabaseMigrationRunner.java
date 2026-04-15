package xyz.refrain.onlineedu.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DatabaseMigrationRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        createScheduleTable();
        createGradeTable();
        ensureScheduleWeekColumns();
        seedDemoDataIfNeeded();
    }

    private void createScheduleTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `edu_course_schedule` ("
                + "`id` int unsigned NOT NULL AUTO_INCREMENT,"
                + "`course_id` int unsigned NOT NULL,"
                + "`day_of_week` tinyint unsigned NOT NULL,"
                + "`section_start` tinyint unsigned NOT NULL,"
                + "`section_end` tinyint unsigned NOT NULL,"
                + "`start_week` tinyint unsigned NOT NULL DEFAULT 1,"
                + "`end_week` tinyint unsigned NOT NULL DEFAULT 21,"
                + "`location` varchar(255) DEFAULT NULL,"
                + "`update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                + "`create_time` datetime DEFAULT CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (`id`),"
                + "KEY `idx_course_weekday_section` (`course_id`,`day_of_week`,`section_start`,`section_end`)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    private void createGradeTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `edu_grade` ("
                + "`id` int unsigned NOT NULL AUTO_INCREMENT,"
                + "`course_id` int unsigned NOT NULL,"
                + "`member_id` int unsigned NOT NULL,"
                + "`score` double DEFAULT NULL,"
                + "`update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                + "`create_time` datetime DEFAULT CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (`id`),"
                + "UNIQUE KEY `uk_course_member` (`course_id`,`member_id`)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    private void ensureScheduleWeekColumns() {
        Integer startWeekColumn = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'edu_course_schedule' AND COLUMN_NAME = 'start_week'",
                Integer.class
        );
        if (startWeekColumn != null && startWeekColumn == 0) {
            jdbcTemplate.execute("ALTER TABLE `edu_course_schedule` ADD COLUMN `start_week` tinyint unsigned NOT NULL DEFAULT 1 AFTER `section_end`");
        }

        Integer endWeekColumn = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'edu_course_schedule' AND COLUMN_NAME = 'end_week'",
                Integer.class
        );
        if (endWeekColumn != null && endWeekColumn == 0) {
            jdbcTemplate.execute("ALTER TABLE `edu_course_schedule` ADD COLUMN `end_week` tinyint unsigned NOT NULL DEFAULT 21 AFTER `start_week`");
        }
    }

    private void seedDemoDataIfNeeded() {
        Integer courseId = jdbcTemplate.query(
                "SELECT id FROM edu_course WHERE enable = 1 ORDER BY id DESC LIMIT 1",
                rs -> rs.next() ? rs.getInt(1) : null
        );
        Integer memberId = jdbcTemplate.query(
                "SELECT id FROM uctr_member WHERE enable = 1 ORDER BY id ASC LIMIT 1",
                rs -> rs.next() ? rs.getInt(1) : null
        );
        if (courseId == null || memberId == null) {
            return;
        }

        Integer scheduleCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM edu_course_schedule", Integer.class);
        if (scheduleCount != null && scheduleCount == 0) {
            jdbcTemplate.update(
                    "INSERT INTO edu_course_schedule(course_id, day_of_week, section_start, section_end, start_week, end_week, location) "
                            + "VALUES (?, 1, 1, 2, 1, 16, 'A101')",
                    courseId
            );
            log.info("Seeded demo schedule for course {}", courseId);
        }

        Integer relationCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM rel_course_member", Integer.class);
        if (relationCount != null && relationCount == 0) {
            jdbcTemplate.update(
                    "INSERT INTO rel_course_member(course_id, member_id) VALUES (?, ?)",
                    courseId, memberId
            );
            log.info("Seeded demo relation for course {} and member {}", courseId, memberId);
        }

        Integer gradeCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM edu_grade", Integer.class);
        if (gradeCount != null && gradeCount == 0) {
            jdbcTemplate.update(
                    "INSERT INTO edu_grade(course_id, member_id, score) VALUES (?, ?, 88)",
                    courseId, memberId
            );
            log.info("Seeded demo grade for course {} and member {}", courseId, memberId);
        }
    }
}

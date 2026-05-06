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
        ensureCourseAdvisorColumns();
        seedCourseAdvisorData();
        seedDefaultTeacherIfNeeded();
        seedDemoTeachersIfNeeded();
        seedAdvisorDemoCoursesIfNeeded();
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

    private void ensureCourseAdvisorColumns() {
        addColumnIfMissing("edu_course", "credit",
                "ALTER TABLE `edu_course` ADD COLUMN `credit` double NOT NULL DEFAULT 2 AFTER `lesson_num`");
        addColumnIfMissing("edu_course", "course_type",
                "ALTER TABLE `edu_course` ADD COLUMN `course_type` varchar(32) NOT NULL DEFAULT 'MAJOR_ELECTIVE' AFTER `credit`");
        addColumnIfMissing("edu_course", "major_name",
                "ALTER TABLE `edu_course` ADD COLUMN `major_name` varchar(64) NOT NULL DEFAULT '计算机科学与技术' AFTER `course_type`");
    }

    private void addColumnIfMissing(String tableName, String columnName, String alterSql) {
        Integer columnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                tableName,
                columnName
        );
        if (columnCount != null && columnCount == 0) {
            jdbcTemplate.execute(alterSql);
        }
    }

    private void seedCourseAdvisorData() {
        jdbcTemplate.update("UPDATE edu_course SET credit = 2 WHERE credit IS NULL OR credit <= 0");
        jdbcTemplate.update("UPDATE edu_course SET course_type = 'MAJOR_ELECTIVE' WHERE course_type IS NULL OR course_type = ''");
        jdbcTemplate.update("UPDATE edu_course SET major_name = '计算机科学与技术' WHERE major_name IS NULL OR major_name = ''");
        jdbcTemplate.update("UPDATE edu_course SET course_type = 'PUBLIC_REQUIRED', credit = 2 WHERE MOD(id, 4) = 1");
        jdbcTemplate.update("UPDATE edu_course SET course_type = 'MAJOR_REQUIRED', credit = 3 WHERE MOD(id, 4) = 2");
        jdbcTemplate.update("UPDATE edu_course SET course_type = 'MAJOR_ELECTIVE', credit = 2 WHERE MOD(id, 4) = 3");
        jdbcTemplate.update("UPDATE edu_course SET course_type = 'GENERAL_ELECTIVE', credit = 2 WHERE MOD(id, 4) = 0");
    }

    private void seedDefaultTeacherIfNeeded() {
        Integer teacherCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM edu_teacher WHERE mobile = '13800138001' OR name = 'teacher'",
                Integer.class
        );
        if (teacherCount != null && teacherCount > 0) {
            jdbcTemplate.update(
                    "UPDATE edu_teacher SET password = '123456', enable = 1, status = 0 "
                            + "WHERE mobile = '13800138001' OR name = 'teacher'"
            );
            return;
        }

        jdbcTemplate.update(
                "INSERT INTO edu_teacher(mobile, email, password, name, intro, avatar, resume, division, sort, enable, status) "
                        + "VALUES ('13800138001', 'teacher@example.com', '123456', "
                        + "'teacher', 'Default demo teacher account', "
                        + "'/api/pub/image/default-teacher.png', '', 80, 0, 1, 0)"
        );
        log.info("Seeded default teacher account 13800138001 / 123456");
    }

    private void seedDemoTeachersIfNeeded() {
        ensureTeacher("13800138002", "teacher_cs@example.com", "teacher_cs", "Computer science demo teacher", 10);
        ensureTeacher("13800138003", "teacher_ai@example.com", "teacher_ai", "AI elective demo teacher", 20);
        ensureTeacher("13800138004", "teacher_public@example.com", "teacher_public", "Public elective demo teacher", 30);
    }

    private void ensureTeacher(String mobile, String email, String name, String intro, int sort) {
        Integer teacherCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM edu_teacher WHERE mobile = ? OR name = ?",
                Integer.class,
                mobile,
                name
        );
        if (teacherCount != null && teacherCount > 0) {
            jdbcTemplate.update(
                    "UPDATE edu_teacher SET password = '123456', email = ?, intro = ?, enable = 1, status = 0, sort = ? "
                            + "WHERE mobile = ? OR name = ?",
                    email,
                    intro,
                    sort,
                    mobile,
                    name
            );
            return;
        }
        jdbcTemplate.update(
                "INSERT INTO edu_teacher(mobile, email, password, name, intro, avatar, resume, division, sort, enable, status) "
                        + "VALUES (?, ?, '123456', ?, ?, '/api/pub/image/default-teacher.png', '', 80, ?, 1, 0)",
                mobile,
                email,
                name,
                intro,
                sort
        );
        log.info("Seeded demo teacher account {} / 123456", mobile);
    }

    private int getTeacherIdByMobile(String mobile, int fallbackTeacherId) {
        Integer teacherId = jdbcTemplate.query(
                "SELECT id FROM edu_teacher WHERE mobile = ? ORDER BY id ASC LIMIT 1",
                rs -> rs.next() ? rs.getInt(1) : null,
                mobile
        );
        return teacherId == null ? fallbackTeacherId : teacherId;
    }

    private void seedAdvisorDemoCoursesIfNeeded() {
        Integer teacherId = jdbcTemplate.query(
                "SELECT id FROM edu_teacher WHERE mobile = '13800138001' OR name = 'teacher' ORDER BY id ASC LIMIT 1",
                rs -> rs.next() ? rs.getInt(1) : null
        );
        Integer subjectId = jdbcTemplate.query(
                "SELECT id FROM edu_subject ORDER BY id ASC LIMIT 1",
                rs -> rs.next() ? rs.getInt(1) : 1
        );
        if (teacherId == null) {
            return;
        }
        ensureDemoCourse("\u9ad8\u7b49\u6570\u5b66A", "PUBLIC_REQUIRED", 4, teacherId, subjectId, 1, 3, 4, "A203");

        ensureDemoCourse("大学英语综合训练", "PUBLIC_REQUIRED", 2, teacherId, subjectId, 1, 1, 2, "A101");
        ensureDemoCourse("数据结构", "MAJOR_REQUIRED", 3, teacherId, subjectId, 2, 3, 4, "B203");
        ensureDemoCourse("操作系统", "MAJOR_REQUIRED", 3, teacherId, subjectId, 3, 1, 2, "B204");
        ensureDemoCourse("人工智能导论", "MAJOR_ELECTIVE", 2, teacherId, subjectId, 4, 5, 6, "C301");
        ensureDemoCourse("数据可视化", "MAJOR_ELECTIVE", 2, teacherId, subjectId, 5, 3, 4, "C302");
        ensureDemoCourse("创新创业基础", "GENERAL_ELECTIVE", 2, teacherId, subjectId, 2, 7, 8, "D105");
        ensureDemoCourse("大学生心理健康", "GENERAL_ELECTIVE", 2, teacherId, subjectId, 5, 7, 8, "D201");
    }

    private void ensureDemoCourse(String title, String courseType, double credit, int teacherId, int subjectId,
                                  int dayOfWeek, int sectionStart, int sectionEnd, String location) {
        int assignedTeacherId = resolveDemoTeacherForCourse(courseType, location, teacherId);
        Integer courseId = jdbcTemplate.query(
                "SELECT id FROM edu_course WHERE title = ? ORDER BY id ASC LIMIT 1",
                rs -> rs.next() ? rs.getInt(1) : null,
                title
        );
        if (courseId == null) {
            jdbcTemplate.update(
                    "INSERT INTO edu_course(teacher_id, subject_id, title, price, lesson_num, credit, course_type, "
                            + "major_name, cover, description, buy_count, view_count, sort, enable, status, remarks) "
                            + "VALUES (?, ?, ?, 0, 32, ?, ?, '计算机科学与技术', '/api/pub/image/default-course.png', "
                            + "?, 0, 0, 0, 1, 1, '')",
                    assignedTeacherId, subjectId, title, credit, courseType,
                    "智能课程管理系统演示课程：" + title
            );
            courseId = jdbcTemplate.query(
                    "SELECT id FROM edu_course WHERE title = ? ORDER BY id ASC LIMIT 1",
                    rs -> rs.next() ? rs.getInt(1) : null,
                    title
            );
        } else {
            jdbcTemplate.update(
                    "UPDATE edu_course SET credit = ?, course_type = ?, major_name = '计算机科学与技术', "
                            + "enable = 1, status = 1 WHERE id = ?",
                    credit, courseType, courseId
            );
        }
        if (courseId == null) {
            return;
        }
        jdbcTemplate.update("UPDATE edu_course SET teacher_id = ? WHERE id = ?", assignedTeacherId, courseId);
        Integer scheduleCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM edu_course_schedule WHERE course_id = ?",
                Integer.class,
                courseId
        );
        if (scheduleCount != null && scheduleCount == 0) {
            jdbcTemplate.update(
                    "INSERT INTO edu_course_schedule(course_id, day_of_week, section_start, section_end, start_week, end_week, location) "
                            + "VALUES (?, ?, ?, ?, 1, 16, ?)",
                    courseId, dayOfWeek, sectionStart, sectionEnd, location
            );
        }
    }

    private int resolveDemoTeacherForCourse(String courseType, String location, int fallbackTeacherId) {
        if ("A203".equals(location)) {
            return fallbackTeacherId;
        }
        if ("PUBLIC_REQUIRED".equals(courseType) || "GENERAL_ELECTIVE".equals(courseType)) {
            return getTeacherIdByMobile("13800138004", fallbackTeacherId);
        }
        if ("MAJOR_ELECTIVE".equals(courseType) || (location != null && location.startsWith("C"))) {
            return getTeacherIdByMobile("13800138003", fallbackTeacherId);
        }
        return getTeacherIdByMobile("13800138002", fallbackTeacherId);
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

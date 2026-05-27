package xyz.refrain.onlineedu.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import xyz.refrain.onlineedu.constant.CacheKeyPrefix;
import xyz.refrain.onlineedu.utils.RedisUtils;

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
        ensureStudentNumberLoginColumn();
        ensureCourseSelectionIndexes();
        ensureCourseAdvisorColumns();
        ensureGradeCompositionColumns();
        seedCourseAdvisorData();
        seedDefaultTeacherIfNeeded();
        seedDemoTeachersIfNeeded();
        seedAdvisorDemoCoursesIfNeeded();
        seedProfessionalDemoDataIfNeeded();
        seedDemoDataIfNeeded();
        clearSubjectCache();
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
                + "`usual_score` double DEFAULT NULL,"
                + "`exam_score` double DEFAULT NULL,"
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

    private void ensureStudentNumberLoginColumn() {
        String columnType = jdbcTemplate.query(
                "SELECT COLUMN_TYPE FROM information_schema.COLUMNS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'uctr_member' AND COLUMN_NAME = 'mobile'",
                rs -> rs.next() ? rs.getString(1) : ""
        );
        if (columnType != null && columnType.toLowerCase().startsWith("char(")) {
            jdbcTemplate.execute("ALTER TABLE `uctr_member` MODIFY COLUMN `mobile` varchar(32) NOT NULL DEFAULT '' COMMENT '学号/登录账号'");
        }
        jdbcTemplate.update("UPDATE uctr_member SET mobile = '222023321102093', nickname = '计科2301-张同学', "
                + "email = '222023321102093@swu.edu.cn', password = '123456' WHERE mobile = '13800138000' OR nickname = '学生'");
    }

    private void ensureCourseSelectionIndexes() {
        dropIndexIfExists("rel_course_member", "idx_course_id");
        dropIndexIfExists("rel_course_member", "idx_member_id");
        Integer uniqueCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.STATISTICS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rel_course_member' "
                        + "AND INDEX_NAME = 'uk_member_course' AND NON_UNIQUE = 0",
                Integer.class
        );
        if (uniqueCount == null || uniqueCount == 0) {
            jdbcTemplate.execute("ALTER TABLE `rel_course_member` ADD UNIQUE KEY `uk_member_course` (`member_id`,`course_id`)");
        }
    }

    private void dropIndexIfExists(String tableName, String indexName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.STATISTICS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?",
                Integer.class,
                tableName,
                indexName
        );
        if (count != null && count > 0) {
            jdbcTemplate.execute("ALTER TABLE `" + tableName + "` DROP INDEX `" + indexName + "`");
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

    private void ensureGradeCompositionColumns() {
        addColumnIfMissing("edu_course", "usual_score_weight",
                "ALTER TABLE `edu_course` ADD COLUMN `usual_score_weight` int NOT NULL DEFAULT 30 AFTER `lesson_num`");
        addColumnIfMissing("edu_course", "exam_score_weight",
                "ALTER TABLE `edu_course` ADD COLUMN `exam_score_weight` int NOT NULL DEFAULT 70 AFTER `usual_score_weight`");
        addColumnIfMissing("edu_grade", "usual_score",
                "ALTER TABLE `edu_grade` ADD COLUMN `usual_score` double DEFAULT NULL AFTER `score`");
        addColumnIfMissing("edu_grade", "exam_score",
                "ALTER TABLE `edu_grade` ADD COLUMN `exam_score` double DEFAULT NULL AFTER `usual_score`");
        jdbcTemplate.update("UPDATE edu_course SET usual_score_weight = 30 WHERE usual_score_weight IS NULL OR usual_score_weight = 0");
        jdbcTemplate.update("UPDATE edu_course SET exam_score_weight = 70 WHERE exam_score_weight IS NULL OR exam_score_weight = 0");
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
                    "UPDATE edu_teacher SET password = '123456', avatar = '/api/pub/image/demo/teacher-avatar-01.jpg', enable = 1, status = 0 "
                            + "WHERE mobile = '13800138001' OR name = 'teacher'"
            );
            return;
        }

        jdbcTemplate.update(
                "INSERT INTO edu_teacher(mobile, email, password, name, intro, avatar, resume, division, sort, enable, status) "
                        + "VALUES ('13800138001', 'teacher@example.com', '123456', "
                        + "'teacher', 'Default demo teacher account', "
                        + "'/api/pub/image/demo/teacher-avatar-01.jpg', '', 80, 0, 1, 0)"
        );
        log.info("Seeded default teacher account 13800138001 / 123456");
    }

    private void seedDemoTeachersIfNeeded() {
        ensureTeacher("13800138002", "teacher_cs@example.com", "teacher_cs", "Computer science demo teacher", 10, "/api/pub/image/demo/teacher-avatar-02.jpg");
        ensureTeacher("13800138003", "teacher_ai@example.com", "teacher_ai", "AI elective demo teacher", 20, "/api/pub/image/demo/teacher-avatar-03.jpg");
        ensureTeacher("13800138004", "teacher_public@example.com", "teacher_public", "Public elective demo teacher", 30, "/api/pub/image/demo/teacher-avatar-04.jpg");
    }

    private void ensureTeacher(String mobile, String email, String name, String intro, int sort, String avatar) {
        Integer teacherCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM edu_teacher WHERE mobile = ? OR name = ?",
                Integer.class,
                mobile,
                name
        );
        if (teacherCount != null && teacherCount > 0) {
            jdbcTemplate.update(
                    "UPDATE edu_teacher SET password = '123456', email = ?, intro = ?, avatar = ?, enable = 1, status = 0, sort = ? "
                            + "WHERE mobile = ? OR name = ?",
                    email,
                    intro,
                    avatar,
                    sort,
                    mobile,
                    name
            );
            return;
        }
        jdbcTemplate.update(
                "INSERT INTO edu_teacher(mobile, email, password, name, intro, avatar, resume, division, sort, enable, status) "
                        + "VALUES (?, ?, '123456', ?, ?, ?, '', 80, ?, 1, 0)",
                mobile,
                email,
                name,
                intro,
                avatar,
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
        int publicRequiredSubjectId = ensureSubject("公共必修", 0, 10);
        int coreSubjectId = ensureSubject("专业核心", 0, 30);
        int electiveSubjectId = ensureSubject("方向选修", 0, 40);
        int generalSubjectId = ensureSubject("通识选修", 0, 60);
        if (teacherId == null) {
            return;
        }
        ensureDemoCourse("\u9ad8\u7b49\u6570\u5b66A", "PUBLIC_REQUIRED", 4, teacherId, publicRequiredSubjectId, 1, 3, 4, "A203");

        ensureDemoCourse("大学英语综合训练", "PUBLIC_REQUIRED", 2, teacherId, publicRequiredSubjectId, 1, 1, 2, "A101");
        ensureDemoCourse("数据结构", "MAJOR_REQUIRED", 3, teacherId, coreSubjectId, 2, 3, 4, "B203");
        ensureDemoCourse("操作系统", "MAJOR_REQUIRED", 3, teacherId, coreSubjectId, 3, 1, 2, "B204");
        ensureDemoCourse("人工智能导论", "MAJOR_ELECTIVE", 2, teacherId, electiveSubjectId, 4, 5, 6, "C301");
        ensureDemoCourse("数据可视化", "MAJOR_ELECTIVE", 2, teacherId, electiveSubjectId, 5, 3, 4, "C302");
        ensureDemoCourse("创新创业基础", "GENERAL_ELECTIVE", 2, teacherId, generalSubjectId, 2, 7, 8, "D105");
        ensureDemoCourse("大学生心理健康", "GENERAL_ELECTIVE", 2, teacherId, generalSubjectId, 5, 7, 8, "D201");
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

    private void seedProfessionalDemoDataIfNeeded() {
        normalizeLegacySubjects();
        ensureSubject("计算机科学与技术", 0, 1);
        int publicRequiredSubjectId = ensureSubject("公共必修", 0, 10);
        int foundationSubjectId = ensureSubject("专业基础", 0, 20);
        int coreSubjectId = ensureSubject("专业核心", 0, 30);
        int electiveSubjectId = ensureSubject("方向选修", 0, 40);
        int practiceSubjectId = ensureSubject("实践创新", 0, 50);
        int generalSubjectId = ensureSubject("通识选修", 0, 60);

        ensureProfessionalCourse("高等数学A", "PUBLIC_REQUIRED", 4, "13800138001", publicRequiredSubjectId,
                1, 3, 4, "A203", 1, 78, 260, "面向计算机专业的数学基础课程，支撑算法分析、建模与后续专业学习。");
        ensureProfessionalCourse("程序设计基础", "MAJOR_REQUIRED", 4, "13800138002", foundationSubjectId,
                1, 5, 6, "B101", 2, 92, 430, "以 C/Java 编程训练为主，建立计算思维、代码规范与调试能力。");
        ensureProfessionalCourse("离散数学", "MAJOR_REQUIRED", 3, "13800138002", foundationSubjectId,
                2, 1, 2, "B102", 3, 66, 310, "覆盖集合、图论、逻辑与组合数学，是数据结构和算法课程的理论基础。");
        ensureProfessionalCourse("数据结构", "MAJOR_REQUIRED", 4, "13800138002", coreSubjectId,
                1, 5, 6, "B203", 4, 118, 620, "系统学习线性表、树、图、查找排序与复杂度分析。");
        ensureProfessionalCourse("计算机组成原理", "MAJOR_REQUIRED", 3, "13800138002", coreSubjectId,
                3, 3, 4, "B205", 5, 74, 360, "理解计算机硬件组织、指令系统、存储层次和 CPU 工作机制。");
        ensureProfessionalCourse("操作系统", "MAJOR_REQUIRED", 3, "13800138002", coreSubjectId,
                3, 1, 2, "B204", 6, 96, 520, "学习进程、线程、内存、文件系统与并发控制。");
        ensureProfessionalCourse("计算机网络", "MAJOR_REQUIRED", 3, "13800138003", coreSubjectId,
                4, 1, 2, "C201", 7, 88, 470, "覆盖 TCP/IP、路由交换、网络应用与基础网络安全。");
        ensureProfessionalCourse("数据库系统原理", "MAJOR_REQUIRED", 3, "13800138003", coreSubjectId,
                4, 3, 4, "C202", 8, 102, 540, "学习关系模型、SQL、事务、索引和数据库设计。");
        ensureProfessionalCourse("人工智能导论", "MAJOR_ELECTIVE", 2, "13800138003", electiveSubjectId,
                1, 5, 6, "C301", 9, 130, 760, "介绍搜索、机器学习、知识表示与智能系统应用。");
        ensureProfessionalCourse("软件工程", "MAJOR_REQUIRED", 3, "13800138004", practiceSubjectId,
                5, 1, 2, "D301", 10, 82, 390, "围绕需求、设计、编码、测试和项目管理组织课程实践。");
        ensureProfessionalCourse("数据可视化", "MAJOR_ELECTIVE", 2, "13800138003", electiveSubjectId,
                5, 3, 4, "C302", 11, 76, 420, "训练数据清洗、图表表达、可视分析和交互设计能力。");
        ensureProfessionalCourse("网络安全基础", "MAJOR_ELECTIVE", 2, "13800138004", electiveSubjectId,
                5, 5, 6, "D401", 12, 69, 350, "覆盖密码学基础、Web 安全、攻防意识和安全开发规范。");
        ensureProfessionalCourse("创新创业基础", "GENERAL_ELECTIVE", 2, "13800138004", generalSubjectId,
                2, 7, 8, "D105", 1, 54, 260, "面向工程项目的创新思维、团队协作与产品意识训练。");
        ensureProfessionalCourse("大学生心理健康", "GENERAL_ELECTIVE", 2, "13800138004", generalSubjectId,
                5, 7, 8, "D201", 2, 61, 280, "帮助学生建立健康学习节奏、压力管理与人际沟通能力。");
        ensureProfessionalCourse("毕业设计智能选题实践", "MAJOR_ELECTIVE", 2, "13800138003", practiceSubjectId,
                6, 3, 4, "LAB-AI", 12, 43, 260, "围绕真实课题进行选题推荐、过程管理和成果归档演示。");

        seedComputerScienceStudents();
        seedComputerScienceSelectionsAndGrades();
        tuneDemoRiskProfiles();
    }

    private void normalizeLegacySubjects() {
        jdbcTemplate.update("UPDATE edu_subject SET title = '计算机科学与技术', sort = 1, enable = 1 WHERE title = 'CS'");
        jdbcTemplate.update("UPDATE edu_subject SET enable = 0 "
                + "WHERE parent_id IN (SELECT id FROM (SELECT id FROM edu_subject WHERE title = '计算机科学与技术' AND parent_id = 0) legacy_parent) "
                + "AND title IN ('专业基础课', '专业核心课', '智能技术方向', '实践创新课')");
        jdbcTemplate.update("UPDATE edu_subject SET enable = 0 WHERE title = '公共通识课' AND parent_id = 0");
    }

    private void clearSubjectCache() {
        try {
            RedisUtils.del(CacheKeyPrefix.CACHE_SUBJECT);
        } catch (Exception ex) {
            log.warn("Skip subject cache clear: {}", ex.getMessage());
        }
    }

    private int ensureSubject(String title, int parentId, int sort) {
        Integer id = jdbcTemplate.query(
                "SELECT id FROM edu_subject WHERE title = ? AND parent_id = ? ORDER BY id ASC LIMIT 1",
                rs -> rs.next() ? rs.getInt(1) : null,
                title,
                parentId
        );
        if (id == null) {
            jdbcTemplate.update(
                    "INSERT INTO edu_subject(title, parent_id, sort, enable) VALUES (?, ?, ?, 1)",
                    title,
                    parentId,
                    sort
            );
            id = jdbcTemplate.query(
                    "SELECT id FROM edu_subject WHERE title = ? AND parent_id = ? ORDER BY id ASC LIMIT 1",
                    rs -> rs.next() ? rs.getInt(1) : null,
                    title,
                    parentId
            );
        } else {
            jdbcTemplate.update("UPDATE edu_subject SET sort = ?, enable = 1 WHERE id = ?", sort, id);
        }
        return id == null ? 1 : id;
    }

    private void ensureProfessionalCourse(String title, String courseType, double credit, String teacherMobile, int subjectId,
                                          int dayOfWeek, int sectionStart, int sectionEnd, String location,
                                          int coverIndex, int buyCount, int viewCount, String description) {
        int teacherId = getTeacherIdByMobile(teacherMobile, 1);
        String cover = String.format("/api/pub/image/demo/course-cover-%02d.jpg", coverIndex);
        Integer courseId = jdbcTemplate.query(
                "SELECT id FROM edu_course WHERE title = ? ORDER BY id ASC LIMIT 1",
                rs -> rs.next() ? rs.getInt(1) : null,
                title
        );
        if (courseId == null) {
            jdbcTemplate.update(
                    "INSERT INTO edu_course(teacher_id, subject_id, title, price, lesson_num, credit, course_type, major_name, "
                            + "cover, description, buy_count, view_count, sort, enable, status, remarks) "
                            + "VALUES (?, ?, ?, 0, 48, ?, ?, '计算机科学与技术', ?, ?, ?, ?, ?, 1, 1, '专业演示课程')",
                    teacherId, subjectId, title, credit, courseType, cover, description, buyCount, viewCount, coverIndex
            );
            courseId = jdbcTemplate.query(
                    "SELECT id FROM edu_course WHERE title = ? ORDER BY id ASC LIMIT 1",
                    rs -> rs.next() ? rs.getInt(1) : null,
                    title
            );
        } else {
            jdbcTemplate.update(
                    "UPDATE edu_course SET teacher_id = ?, subject_id = ?, credit = ?, course_type = ?, major_name = '计算机科学与技术', "
                            + "cover = ?, description = ?, buy_count = ?, view_count = ?, sort = ?, enable = 1, status = 1 WHERE id = ?",
                    teacherId, subjectId, credit, courseType, cover, description, buyCount, viewCount, coverIndex, courseId
            );
        }
        if (courseId != null) {
            jdbcTemplate.update("DELETE FROM edu_course_schedule WHERE course_id = ?", courseId);
            jdbcTemplate.update(
                    "INSERT INTO edu_course_schedule(course_id, day_of_week, section_start, section_end, start_week, end_week, location) "
                            + "VALUES (?, ?, ?, ?, 1, 16, ?)",
                    courseId, dayOfWeek, sectionStart, sectionEnd, location
            );
        }
    }

    private void seedComputerScienceStudents() {
        String[] names = {"张明", "李思雨", "王子涵", "赵一鸣", "陈嘉琪", "刘浩然", "周可欣", "黄俊杰",
                "吴雨桐", "徐子墨", "孙若曦", "胡嘉诚", "林语晨", "郭航", "何清扬", "马欣怡"};
        long base = 222023321102093L;
        for (int i = 0; i < names.length; i++) {
            String studentNo = String.valueOf(base + i);
            String nickname = "计科2301-" + names[i];
            String avatar = "";
            Integer sex = i % 3 == 0 ? 2 : 1;
            jdbcTemplate.update(
                    "INSERT INTO uctr_member(mobile, email, password, nickname, sex, age, avatar, sign, enable) "
                            + "VALUES (?, ?, '123456', ?, ?, 20, ?, '计算机科学与技术2023级演示学生', 1) "
                            + "ON DUPLICATE KEY UPDATE email = VALUES(email), password = '123456', nickname = VALUES(nickname), "
                            + "sex = VALUES(sex), avatar = VALUES(avatar), sign = VALUES(sign), enable = 1",
                    studentNo,
                    studentNo + "@swu.edu.cn",
                    nickname,
                    sex,
                    avatar
            );
        }
    }

    private void seedComputerScienceSelectionsAndGrades() {
        String[] courses = {"高等数学A", "程序设计基础", "离散数学", "数据结构", "计算机组成原理", "操作系统",
                "计算机网络", "数据库系统原理", "人工智能导论", "软件工程", "数据可视化", "网络安全基础",
                "创新创业基础", "大学生心理健康", "毕业设计智能选题实践"};
        long base = 222023321102093L;
        for (int studentIndex = 0; studentIndex < 16; studentIndex++) {
            Integer memberId = getMemberId(String.valueOf(base + studentIndex));
            if (memberId == null) {
                continue;
            }
            int selectedCount = 8 + (studentIndex % 6);
            for (int courseIndex = 0; courseIndex < Math.min(selectedCount, courses.length); courseIndex++) {
                Integer courseId = getCourseId(courses[courseIndex]);
                if (courseId == null) {
                    continue;
                }
                jdbcTemplate.update(
                        "INSERT IGNORE INTO rel_course_member(course_id, member_id) VALUES (?, ?)",
                        courseId,
                        memberId
                );
                if (courseIndex < 7 + (studentIndex % 4)) {
                    double score = 58 + ((studentIndex * 7 + courseIndex * 5) % 40);
                    if (studentIndex == 0) {
                        score = 78 + (courseIndex % 5) * 3;
                    }
                    jdbcTemplate.update(
                            "INSERT INTO edu_grade(course_id, member_id, score) VALUES (?, ?, ?) "
                                    + "ON DUPLICATE KEY UPDATE score = VALUES(score)",
                            courseId,
                            memberId,
                            Math.min(score, 98)
                    );
                }
            }
        }
    }

    private void tuneDemoRiskProfiles() {
        java.util.List<Integer> demoMembers = jdbcTemplate.queryForList(
                "SELECT id FROM uctr_member WHERE mobile BETWEEN '222023321102093' AND '222023321102108' ORDER BY mobile",
                Integer.class
        );
        if (demoMembers.size() < 12) {
            return;
        }
        java.util.List<Integer> publicRequired = courseIdsByTitles("高等数学A");
        if (publicRequired.isEmpty()) {
            publicRequired = courseIdsByType("PUBLIC_REQUIRED");
        }
        java.util.List<Integer> majorRequiredFull = courseIdsByTitles(
                "程序设计基础", "离散数学", "数据结构", "计算机组成原理", "操作系统", "计算机网络");
        java.util.List<Integer> majorRequiredPartial = courseIdsByTitles("程序设计基础", "离散数学");
        java.util.List<Integer> majorElectiveFull = courseIdsByTitles("人工智能导论", "数据可视化", "网络安全基础");
        java.util.List<Integer> majorElectivePartial = courseIdsByTitles("人工智能导论");
        java.util.List<Integer> generalElectiveFull = courseIdsByTitles("创新创业基础", "大学生心理健康");
        for (int i = 0; i < demoMembers.size(); i++) {
            Integer memberId = demoMembers.get(i);
            jdbcTemplate.update("DELETE FROM edu_grade WHERE member_id = ?", memberId);
            jdbcTemplate.update("DELETE FROM rel_course_member WHERE member_id = ?", memberId);
            if (i < 5) {
                enrollCourses(memberId, publicRequired, 0, publicRequired.size(), true, 82 + i);
                enrollCourses(memberId, majorRequiredFull, 0, majorRequiredFull.size(), true, 82 + i);
                enrollCourses(memberId, majorElectiveFull, 0, majorElectiveFull.size(), true, 82 + i);
                enrollCourses(memberId, generalElectiveFull, 0, generalElectiveFull.size(), true, 82 + i);
            } else if (i < 10) {
                enrollCourses(memberId, publicRequired, 0, publicRequired.size(), true, 76 + i);
                enrollCourses(memberId, majorRequiredFull, 0, majorRequiredFull.size(), true, 72 + i);
                enrollCourses(memberId, majorElectivePartial, 0, majorElectivePartial.size(), true, 70 + i);
                enrollCourses(memberId, generalElectiveFull, 0, generalElectiveFull.size(), true, 70 + i);
            } else {
                enrollCourses(memberId, publicRequired, 0, Math.min(1, publicRequired.size()), i % 2 == 0, 55);
                enrollCourses(memberId, majorRequiredPartial, 0, majorRequiredPartial.size(), i % 3 == 0, 54);
            }
        }
    }

    private java.util.List<Integer> courseIdsByType(String courseType) {
        return jdbcTemplate.queryForList(
                "SELECT id FROM edu_course WHERE enable = 1 AND status = 1 AND course_type = ? ORDER BY sort, id",
                Integer.class,
                courseType
        );
    }

    private java.util.List<Integer> courseIdsByTitles(String... titles) {
        java.util.List<Integer> ids = new java.util.ArrayList<>();
        for (String title : titles) {
            Integer id = getCourseId(title);
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }

    private void enrollCourses(Integer memberId, java.util.List<Integer> courseIds, int start, int end, boolean pass, int baseScore) {
        for (int i = start; i < end && i < courseIds.size(); i++) {
            Integer courseId = courseIds.get(i);
            jdbcTemplate.update("INSERT IGNORE INTO rel_course_member(course_id, member_id) VALUES (?, ?)", courseId, memberId);
            int score = pass ? Math.min(98, baseScore + (i % 8)) : Math.max(35, baseScore - (i % 6));
            jdbcTemplate.update(
                    "INSERT INTO edu_grade(course_id, member_id, score) VALUES (?, ?, ?) "
                            + "ON DUPLICATE KEY UPDATE score = VALUES(score)",
                    courseId,
                    memberId,
                    score
            );
        }
    }

    private Integer getMemberId(String studentNo) {
        return jdbcTemplate.query(
                "SELECT id FROM uctr_member WHERE mobile = ? ORDER BY id ASC LIMIT 1",
                rs -> rs.next() ? rs.getInt(1) : null,
                studentNo
        );
    }

    private Integer getCourseId(String title) {
        return jdbcTemplate.query(
                "SELECT id FROM edu_course WHERE title = ? ORDER BY id ASC LIMIT 1",
                rs -> rs.next() ? rs.getInt(1) : null,
                title
        );
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

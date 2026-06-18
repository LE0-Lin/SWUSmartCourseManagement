package xyz.refrain.onlineedu.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import xyz.refrain.onlineedu.constant.CacheKeyPrefix;
import xyz.refrain.onlineedu.utils.RedisUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        seedCurriculumDemoData();
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
        addColumnIfMissing("edu_course", "course_code",
                "ALTER TABLE `edu_course` ADD COLUMN `course_code` varchar(32) NOT NULL DEFAULT '' AFTER `lesson_num`");
        addColumnIfMissing("edu_course", "credit",
                "ALTER TABLE `edu_course` ADD COLUMN `credit` double NOT NULL DEFAULT 2 AFTER `course_code`");
        addColumnIfMissing("edu_course", "course_type",
                "ALTER TABLE `edu_course` ADD COLUMN `course_type` varchar(32) NOT NULL DEFAULT 'MAJOR_ELECTIVE' AFTER `credit`");
        addColumnIfMissing("edu_course", "major_name",
                "ALTER TABLE `edu_course` ADD COLUMN `major_name` varchar(64) NOT NULL DEFAULT '计算机科学与技术（中外合作办学）' AFTER `course_type`");
        addColumnIfMissing("edu_course", "curriculum_semester",
                "ALTER TABLE `edu_course` ADD COLUMN `curriculum_semester` varchar(32) NOT NULL DEFAULT '' AFTER `major_name`");
        addColumnIfMissing("edu_course", "assessment_method",
                "ALTER TABLE `edu_course` ADD COLUMN `assessment_method` varchar(32) NOT NULL DEFAULT '' AFTER `curriculum_semester`");
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
        jdbcTemplate.update("UPDATE edu_course SET major_name = ? WHERE major_name IS NULL OR major_name = ''",
                CurriculumCourseCatalog.MAJOR_NAME);
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

    private void seedCurriculumDemoData() {
        normalizeLegacySubjects();
        disableLegacyDemoCourses();

        Map<String, Integer> subjectIds = new LinkedHashMap<>();
        subjectIds.put("通识教育必修", ensureSubject("通识教育必修", 0, 10));
        subjectIds.put("通识教育选修", ensureSubject("通识教育选修", 0, 20));
        subjectIds.put("学科基础", ensureSubject("学科基础", 0, 30));
        subjectIds.put("专业发展必修", ensureSubject("专业发展必修", 0, 40));
        subjectIds.put("专业发展选修", ensureSubject("专业发展选修", 0, 50));
        subjectIds.put("综合实践", ensureSubject("综合实践", 0, 60));
        jdbcTemplate.update("UPDATE edu_subject SET enable = 0 WHERE parent_id = 0 AND title IN "
                + "('计算机科学与技术', '公共必修', '专业基础', '专业核心', '方向选修', '实践创新', '通识选修', '公共通识课')");

        int sort = 1;
        int electiveScheduleIndex = 0;
        for (CurriculumCourseCatalog.CourseSeed seed : CurriculumCourseCatalog.all()) {
            Integer subjectId = subjectIds.get(seed.category);
            if (subjectId == null) {
                continue;
            }
            Integer courseId = upsertCurriculumCourse(seed, subjectId, sort++);
            if (courseId == null) {
                continue;
            }
            jdbcTemplate.update("DELETE FROM edu_course_schedule WHERE course_id = ?", courseId);
            if ("MAJOR_ELECTIVE".equals(seed.courseType)) {
                seedCurriculumElectiveSchedule(courseId, electiveScheduleIndex++);
            }
        }

        seedComputerScienceStudents();
        tuneCurriculumRiskProfiles();
        log.info("Seeded {} curriculum-based demo course records", CurriculumCourseCatalog.all().size());
    }

    private void disableLegacyDemoCourses() {
        jdbcTemplate.update("UPDATE edu_course SET enable = 0 WHERE remarks = '专业演示课程' "
                + "OR remarks LIKE '培养方案课程%' OR title IN "
                + "('画图', '高等数学A', '大学英语综合训练', '程序设计基础', '离散数学', '数据结构', "
                + "'计算机组成原理', '操作系统', '计算机网络', '数据库系统原理', '人工智能导论', "
                + "'软件工程', '数据可视化', '网络安全基础', '创新创业基础', '大学生心理健康', "
                + "'毕业设计智能选题实践')");
    }

    private Integer upsertCurriculumCourse(CurriculumCourseCatalog.CourseSeed seed, int subjectId, int sort) {
        Integer courseId = null;
        if (seed.code != null && !seed.code.isEmpty()) {
            courseId = jdbcTemplate.query(
                    "SELECT id FROM edu_course WHERE course_code = ? ORDER BY id ASC LIMIT 1",
                    rs -> rs.next() ? rs.getInt(1) : null,
                    seed.code
            );
        }
        if (courseId == null) {
            courseId = jdbcTemplate.query(
                    "SELECT id FROM edu_course WHERE title = ? ORDER BY id ASC LIMIT 1",
                    rs -> rs.next() ? rs.getInt(1) : null,
                    seed.title
            );
        }

        int teacherId = getTeacherIdByMobile(seed.teacherMobile, 1);
        int buyCount = 18 + (sort * 7 % 113);
        int viewCount = 96 + (sort * 19 % 701);
        String description = "依据西南大学计算机科学与技术（中外合作办学）2022版培养方案录入。"
                + "建议第 " + seed.curriculumSemester + " 学期开设，共 " + formatCredit(seed.credit)
                + " 学分，" + seed.lessonNum + " 学时，考核方式：" + seed.assessmentMethod + "。";
        if (courseId == null) {
            jdbcTemplate.update(
                    "INSERT INTO edu_course(teacher_id, subject_id, course_code, title, price, lesson_num, credit, "
                            + "course_type, major_name, curriculum_semester, assessment_method, cover, description, "
                            + "buy_count, view_count, sort, enable, status, remarks) "
                            + "VALUES (?, ?, ?, ?, 0, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 1, ?)",
                    teacherId, subjectId, seed.code, seed.title, seed.lessonNum, seed.credit, seed.courseType,
                    CurriculumCourseCatalog.MAJOR_NAME, seed.curriculumSemester, seed.assessmentMethod, seed.cover,
                    description, buyCount, viewCount, sort, seed.remarks
            );
            courseId = jdbcTemplate.query(
                    "SELECT id FROM edu_course WHERE title = ? ORDER BY id DESC LIMIT 1",
                    rs -> rs.next() ? rs.getInt(1) : null,
                    seed.title
            );
        } else {
            jdbcTemplate.update(
                    "UPDATE edu_course SET teacher_id = ?, subject_id = ?, course_code = ?, title = ?, lesson_num = ?, "
                            + "credit = ?, course_type = ?, major_name = ?, curriculum_semester = ?, assessment_method = ?, "
                            + "cover = ?, description = ?, buy_count = ?, view_count = ?, sort = ?, enable = 1, status = 1, "
                            + "remarks = ? WHERE id = ?",
                    teacherId, subjectId, seed.code, seed.title, seed.lessonNum, seed.credit, seed.courseType,
                    CurriculumCourseCatalog.MAJOR_NAME, seed.curriculumSemester, seed.assessmentMethod, seed.cover,
                    description, buyCount, viewCount, sort, seed.remarks, courseId
            );
        }
        return courseId;
    }

    private void seedCurriculumElectiveSchedule(int courseId, int index) {
        int dayOfWeek = index % 5 + 1;
        int sectionStart = index / 5 * 2 + 1;
        // Keep two controlled overlaps so the advisor demo can visibly prove that
        // it filters both current-timetable conflicts and conflicts inside a path plan.
        if (index == 19) {
            dayOfWeek = 4;
        } else if (index == 20) {
            dayOfWeek = 1;
            sectionStart = 1;
        }
        int sectionEnd = sectionStart + 1;
        String location = "C" + (301 + index % 12);
        jdbcTemplate.update(
                "INSERT INTO edu_course_schedule(course_id, day_of_week, section_start, section_end, start_week, end_week, location) "
                        + "VALUES (?, ?, ?, ?, 1, 16, ?)",
                courseId, dayOfWeek, sectionStart, sectionEnd, location
        );
    }

    private void tuneCurriculumRiskProfiles() {
        List<Integer> demoMembers = jdbcTemplate.queryForList(
                "SELECT id FROM uctr_member WHERE mobile BETWEEN '222023321102093' AND '222023321102108' ORDER BY mobile",
                Integer.class
        );
        if (demoMembers.size() < 12) {
            return;
        }
        List<Integer> publicRequired = courseIdsByType("PUBLIC_REQUIRED");
        List<Integer> generalElective = courseIdsByType("GENERAL_ELECTIVE");
        List<Integer> disciplineRequired = courseIdsByType("DISCIPLINE_REQUIRED");
        List<Integer> majorRequired = courseIdsByType("MAJOR_REQUIRED");
        List<Integer> majorElective = courseIdsByType("MAJOR_ELECTIVE");
        List<Integer> practiceRequired = courseIdsByType("PRACTICE_REQUIRED");
        for (int i = 0; i < demoMembers.size(); i++) {
            Integer memberId = demoMembers.get(i);
            jdbcTemplate.update("DELETE FROM edu_grade WHERE member_id = ?", memberId);
            jdbcTemplate.update("DELETE FROM rel_course_member WHERE member_id = ?", memberId);
            if (i < 5) {
                if (i == 1) {
                    enrollRequiredCourses(memberId, publicRequired, disciplineRequired, majorRequired, practiceRequired, 85);
                    enrollPendingCourses(memberId, generalElective, 0, generalElective.size());
                    enrollCoursesUntilCredits(memberId, majorElective, 17, true, 84);
                } else {
                    enrollCompleteProfile(memberId, publicRequired, generalElective, disciplineRequired,
                            majorRequired, majorElective, practiceRequired, 84 + i);
                }
            } else if (i < 10) {
                enrollRequiredCourses(memberId, publicRequired, disciplineRequired, majorRequired, practiceRequired, 76 + i);
                if (i == 5) {
                    enrollCourses(memberId, generalElective, 0, generalElective.size(), true, 76 + i);
                    enrollCoursesUntilCredits(memberId, majorElective, 13.5, true, 72 + i);
                } else if (i == 6) {
                    enrollCourses(memberId, generalElective, 0, Math.min(2, generalElective.size()), true, 76 + i);
                    enrollCoursesUntilCredits(memberId, majorElective, 17, true, 72 + i);
                } else if (i == 7) {
                    enrollCourses(memberId, generalElective, 0, generalElective.size(), true, 76 + i);
                    enrollCoursesUntilCredits(memberId, majorElective, 16.5, true, 72 + i);
                } else if (i == 8) {
                    enrollCourses(memberId, generalElective, 0, Math.min(3, generalElective.size()), true, 76 + i);
                    enrollCoursesUntilCredits(memberId, majorElective, 13.5, true, 72 + i);
                } else {
                    enrollCourses(memberId, generalElective, 0, Math.min(3, generalElective.size()), true, 76 + i);
                    enrollCoursesUntilCredits(memberId, majorElective, 17, true, 72 + i);
                }
            } else {
                if (i == 10) {
                    enrollCourses(memberId, publicRequired, 0, Math.min(6, publicRequired.size()), true, 68);
                    enrollCourses(memberId, disciplineRequired, 0, Math.min(2, disciplineRequired.size()), true, 66);
                } else if (i == 11) {
                    // Deliberately empty transcript: useful for the maximum-gap dashboard.
                } else if (i == 12) {
                    enrollCourses(memberId, publicRequired, 0, publicRequired.size(), true, 78);
                    enrollCourses(memberId, generalElective, 0, generalElective.size(), true, 78);
                    enrollCourses(memberId, disciplineRequired, 0, Math.max(0, disciplineRequired.size() - 1), true, 76);
                    enrollCourses(memberId, majorRequired, 0, majorRequired.size(), true, 76);
                    enrollCourses(memberId, practiceRequired, 0, practiceRequired.size(), true, 80);
                    enrollCoursesUntilCredits(memberId, majorElective, 17, true, 77);
                } else if (i == 13) {
                    enrollCompleteProfile(memberId, publicRequired, generalElective, disciplineRequired,
                            majorRequired, majorElective, practiceRequired, 77);
                    enrollCourses(memberId, disciplineRequired, disciplineRequired.size() - 1,
                            disciplineRequired.size(), false, 55);
                } else if (i == 14) {
                    enrollRequiredCourses(memberId, publicRequired, disciplineRequired, majorRequired, practiceRequired, 78);
                    enrollCoursesUntilCredits(memberId, majorElective, 17, true, 77);
                } else {
                    enrollCourses(memberId, publicRequired, 0, publicRequired.size(), true, 78);
                    enrollCourses(memberId, generalElective, 0, generalElective.size(), true, 78);
                    enrollCourses(memberId, disciplineRequired, 0, disciplineRequired.size(), true, 76);
                    enrollCourses(memberId, majorRequired, 0, majorRequired.size(), true, 76);
                    enrollCourses(memberId, practiceRequired, 0, Math.min(7, practiceRequired.size()), true, 80);
                    enrollCoursesUntilCredits(memberId, majorElective, 17, true, 77);
                }
            }
        }
    }

    private void enrollCompleteProfile(Integer memberId, List<Integer> publicRequired, List<Integer> generalElective,
                                       List<Integer> disciplineRequired, List<Integer> majorRequired,
                                       List<Integer> majorElective, List<Integer> practiceRequired, int baseScore) {
        enrollRequiredCourses(memberId, publicRequired, disciplineRequired, majorRequired, practiceRequired, baseScore);
        enrollCourses(memberId, generalElective, 0, generalElective.size(), true, baseScore);
        enrollCoursesUntilCredits(memberId, majorElective, 17, true, baseScore - 1);
    }

    private void enrollRequiredCourses(Integer memberId, List<Integer> publicRequired, List<Integer> disciplineRequired,
                                       List<Integer> majorRequired, List<Integer> practiceRequired, int baseScore) {
        enrollCourses(memberId, publicRequired, 0, publicRequired.size(), true, baseScore);
        enrollCourses(memberId, disciplineRequired, 0, disciplineRequired.size(), true, baseScore - 2);
        enrollCourses(memberId, majorRequired, 0, majorRequired.size(), true, baseScore - 2);
        enrollCourses(memberId, practiceRequired, 0, practiceRequired.size(), true, baseScore + 2);
    }

    private void enrollPendingCourses(Integer memberId, List<Integer> courseIds, int start, int end) {
        for (int i = start; i < end && i < courseIds.size(); i++) {
            jdbcTemplate.update("INSERT IGNORE INTO rel_course_member(course_id, member_id) VALUES (?, ?)",
                    courseIds.get(i), memberId);
        }
    }

    private void enrollCoursesUntilCredits(Integer memberId, List<Integer> courseIds, double targetCredits,
                                           boolean pass, int baseScore) {
        double credits = 0;
        for (int i = 0; i < courseIds.size() && credits < targetCredits; i++) {
            Integer courseId = courseIds.get(i);
            enrollCourses(memberId, courseIds, i, i + 1, pass, baseScore);
            Double credit = jdbcTemplate.queryForObject("SELECT credit FROM edu_course WHERE id = ?", Double.class, courseId);
            credits += credit == null ? 0 : credit;
        }
    }

    private String formatCredit(double value) {
        return value == Math.floor(value) ? String.valueOf((int) value) : String.valueOf(value);
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

    private java.util.List<Integer> courseIdsByType(String courseType) {
        return jdbcTemplate.queryForList(
                "SELECT id FROM edu_course WHERE enable = 1 AND status = 1 AND course_type = ? ORDER BY sort, id",
                Integer.class,
                courseType
        );
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

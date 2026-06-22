package xyz.refrain.onlineedu.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Actual SQL against an isolated in-memory schema; no running application or local demo database. */
@SuppressWarnings("unchecked")
class SmartAdvisorServiceTest {
    private CountingJdbcTemplate jdbc;
    private SmartAdvisorService advisor;

    @AfterEach
    void releaseFixtureDatabase() {
        if (jdbc != null) jdbc.execute("SHUTDOWN");
    }

    @BeforeEach
    void createIsolatedCatalog() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:advisor_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbc = new CountingJdbcTemplate(dataSource);
        advisor = new SmartAdvisorService(jdbc);
        jdbc.execute("CREATE TABLE edu_teacher(id INT PRIMARY KEY, name VARCHAR(50))");
        jdbc.execute("CREATE TABLE edu_course(id INT PRIMARY KEY, title VARCHAR(100), credit DOUBLE, course_type VARCHAR(50), major_name VARCHAR(100), buy_count INT DEFAULT 0, view_count INT DEFAULT 0, teacher_id INT, enable INT DEFAULT 1, status INT DEFAULT 1)");
        jdbc.execute("CREATE TABLE rel_course_member(course_id INT, member_id INT)");
        jdbc.execute("CREATE TABLE edu_grade(course_id INT, member_id INT, score DOUBLE)");
        jdbc.execute("CREATE TABLE edu_course_schedule(course_id INT, day_of_week INT, section_start INT, section_end INT, start_week INT, end_week INT, location VARCHAR(50))");
        String[] types = {"PUBLIC_REQUIRED", "GENERAL_ELECTIVE", "DISCIPLINE_REQUIRED", "MAJOR_REQUIRED", "MAJOR_ELECTIVE", "PRACTICE_REQUIRED"};
        double[] credits = {37, 8, 33.5, 32.5, 16.5, 32};
        for (int i = 0; i < types.length; i++) {
            course(i + 1, types[i], credits[i], 0, true);
            jdbc.update("INSERT INTO rel_course_member VALUES (?, 1)", i + 1);
            jdbc.update("INSERT INTO edu_grade VALUES (?, 1, 80)", i + 1);
        }
    }

    @Test
    void findsTheExactGapCourseBeyondTheTwelveVisibleCardsAndUsesTwoScheduleQueries() throws Exception {
        for (int id = 100; id < 112; id++) course(id, "MAJOR_ELECTIVE", 3, 1000, true);
        course(112, "MAJOR_ELECTIVE", 0.5, 0, true);
        Map<String, Object> data = dashboard();
        List<Map<String, Object>> visible = (List<Map<String, Object>>) data.get("recommendations");
        List<Map<String, Object>> path = (List<Map<String, Object>>) data.get("pathPlan");
        assertEquals(12, visible.size());
        assertFalse(visible.stream().anyMatch(item -> ((Number) item.get("courseId")).intValue() == 112));
        assertEquals(1, path.size());
        assertEquals(112, ((Number) path.get(0).get("courseId")).intValue());
        assertEquals(13, ((Number) planning(data).get("candidateCount")).intValue());
        assertEquals("COMPLETE", planning(data).get("status"));
        assertEquals(2, jdbc.scheduleQueries, "Schedule reads must not grow with candidate count");
        writeFixture("full-catalog", data);
    }

    @Test
    void doesNotTreatMissingCandidateSchedulesAsEvidenceOfNoConflict() throws Exception {
        course(100, "MAJOR_ELECTIVE", 0.5, 0, false);
        Map<String, Object> data = dashboard();
        assertEquals("INCOMPLETE_DATA", planning(data).get("status"));
        assertEquals(Boolean.FALSE, planning(data).get("inputComplete"));
        assertTrue(((List<?>) data.get("pathPlan")).isEmpty());
        Map<String, Object> diagnosis = (Map<String, Object>) advisor.diagnoseCourse(1, 100).getData();
        assertEquals("SCHEDULE_UNKNOWN", diagnosis.get("code"));
        assertEquals(Boolean.FALSE, diagnosis.get("selectable"));
        writeFixture("missing-candidate", data);
    }

    @Test
    void invalidWeekIntervalsAreReportedAsUnknown() {
        course(100, "MAJOR_ELECTIVE", 0.5, 0, true);
        jdbc.update("UPDATE edu_course_schedule SET start_week = 12, end_week = 3 WHERE course_id = 100");
        assertEquals("INCOMPLETE_DATA", planning(dashboard()).get("status"));
    }

    @Test
    void withholdsAPlanWhenAnExistingSelectedCourseHasNoTimetable() throws Exception {
        course(100, "MAJOR_ELECTIVE", 0.5, 0, true);
        jdbc.update("DELETE FROM edu_course_schedule WHERE course_id = 1");
        Map<String, Object> data = dashboard();
        assertEquals("INSUFFICIENT_SCHEDULES", planning(data).get("status"));
        assertEquals(Boolean.FALSE, planning(data).get("searchComplete"));
        assertTrue(((List<?>) data.get("pathPlan")).isEmpty());
        writeFixture("missing-selected", data);
    }

    @Test
    void surplusCreditsInAnotherBucketCannotConcealTheHalfCreditGap() {
        jdbc.update("UPDATE edu_course SET credit = 100 WHERE id = 1");
        Map<String, Object> summary = (Map<String, Object>) dashboard().get("summary");
        assertEquals(0.5, ((Number) summary.get("projectedRemainingCredits")).doubleValue());
        assertTrue(((Number) summary.get("projectedRate")).doubleValue() < 100);
    }

    @Test
    void failedCoursesDoNotCountAsProjectedCreditsButPendingCoursesDo() {
        jdbc.update("UPDATE edu_grade SET score = 59 WHERE course_id = 2");
        Map<String, Object> data = dashboard();
        assertEquals(8.5, ((Number) ((Map<?, ?>) data.get("summary")).get("projectedRemainingCredits")).doubleValue());
        jdbc.update("DELETE FROM edu_grade WHERE course_id = 2");
        data = dashboard();
        assertEquals(0.5, ((Number) ((Map<?, ?>) data.get("summary")).get("projectedRemainingCredits")).doubleValue());
    }

    @Test
    void missingCreditsAreNotReplacedByAnInventedTwoCreditContribution() {
        course(100, "MAJOR_ELECTIVE", null, 0, true);
        Map<String, Object> data = dashboard();
        assertEquals("INCOMPLETE_DATA", planning(data).get("status"));
        assertTrue(((List<?>) data.get("pathPlan")).isEmpty());
    }

    @Test
    void missingCourseIsReportedAsNotFound() {
        assertEquals("NOT_FOUND", ((Map<?, ?>) advisor.diagnoseCourse(1, 999).getData()).get("code"));
    }

    @Test
    void missingSelectedCreditsWithholdAPlanInsteadOfGuessingCreditTotals() {
        jdbc.update("UPDATE edu_course SET credit = NULL WHERE id = 1");
        course(100, "MAJOR_ELECTIVE", 0.5, 0, true);
        Map<String, Object> data = dashboard();
        assertEquals("INSUFFICIENT_CREDITS", planning(data).get("status"));
        assertEquals(Boolean.FALSE, planning(data).get("inputComplete"));
        assertTrue(((List<?>) data.get("pathPlan")).isEmpty());
    }

    @Test
    void fullCatalogSearchExposesTheBudgetLimitWithoutClaimingOptimality() throws Exception {
        for (int id = 100; id < 145; id++) {
            course(id, "MAJOR_ELECTIVE", 0.1, 0, true);
            int offset = id - 100;
            int week = 1 + offset % 15;
            jdbc.update("UPDATE edu_course_schedule SET day_of_week = ?, start_week = ?, end_week = ? WHERE course_id = ?",
                    1 + offset / 15, week, week, id);
        }
        Map<String, Object> data = dashboard();
        Map<String, Object> report = planning(data);
        assertEquals("SEARCH_LIMIT", report.get("status"));
        assertEquals(Boolean.FALSE, report.get("searchComplete"));
        assertEquals(Boolean.TRUE, report.get("inputComplete"));
        assertEquals(100000, ((Number) report.get("exploredStates")).intValue());
        assertEquals(4, ((List<?>) data.get("pathPlan")).size());
        assertEquals(2, jdbc.scheduleQueries);
        writeFixture("search-limit", data);
    }

    private void writeFixture(String name, Map<String, Object> data) throws Exception {
        Path fixture = Paths.get("target/demo/advisor-" + name + ".json");
        Files.createDirectories(fixture.getParent());
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(fixture.toFile(), data);
    }

    private Map<String, Object> dashboard() { return (Map<String, Object>) advisor.getStudentDashboard(1).getData(); }
    private Map<String, Object> planning(Map<String, Object> data) { return (Map<String, Object>) data.get("planning"); }

    private void course(int id, String type, double credit, int popularity, boolean scheduled) {
        course(id, type, Double.valueOf(credit), popularity, scheduled);
    }

    private void course(int id, String type, Double credit, int popularity, boolean scheduled) {
        jdbc.update("INSERT INTO edu_course(id, title, credit, course_type, major_name, buy_count) VALUES (?, ?, ?, ?, 'Fixture major', ?)",
                id, "Fixture course " + id, credit, type, popularity);
        if (scheduled) jdbc.update("INSERT INTO edu_course_schedule VALUES (?, ?, 1, 2, 1, 21, 'Fixture room')", id, id < 100 ? 7 : 1);
    }

    private static class CountingJdbcTemplate extends JdbcTemplate {
        private int scheduleQueries;
        private CountingJdbcTemplate(JdbcDataSource dataSource) { super(dataSource); }
        @Override
        public List<Map<String, Object>> queryForList(String sql, Object... args) {
            if (sql.contains("FROM edu_course_schedule")) scheduleQueries++;
            return super.queryForList(sql, args);
        }
    }
}

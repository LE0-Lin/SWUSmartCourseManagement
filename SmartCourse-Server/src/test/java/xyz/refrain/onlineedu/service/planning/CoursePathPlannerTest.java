package xyz.refrain.onlineedu.service.planning;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class CoursePathPlannerTest {
    private final CoursePathPlanner planner = new CoursePathPlanner(4, 100000);

    @Test
    void minimizesGapThenExcessThenCountInsteadOfTakingTheFirstPopularCourse() {
        List<CoursePathPlanner.Course> courses = Arrays.asList(course(1, "A", "6", 0, slot(1)),
                course(2, "A", "4", 1, slot(2)), course(3, "B", "4", 2, slot(3)));
        CoursePathPlanner.Result result = planner.plan(gaps("4", "4"), courses, Collections.emptyList());
        assertEquals(Arrays.asList(2, 3), result.getCourseIds());
        assertEquals(0, result.getRemainingCredits().signum());
        assertEquals(0, result.getExcessCredits().signum());
        assertTrue(result.isSearchComplete());
    }

    @Test
    void decimalCreditArithmeticDoesNotCreateAPhantomGap() {
        List<CoursePathPlanner.Course> courses = Arrays.asList(course(1, "A", "0.1", 0, slot(1)),
                course(2, "A", "0.2", 1, slot(2)));
        CoursePathPlanner.Result result = planner.plan(gaps("0.3", "0"), courses, Collections.emptyList());
        assertEquals(0, result.getRemainingCredits().signum());
        assertEquals(0, result.getExcessCredits().signum());
    }

    @Test
    void checksConflictsWithBothTheExistingTimetableAndTheProposedCourses() {
        List<CoursePathPlanner.Course> courses = Arrays.asList(course(1, "A", "1", 0, slot(1)),
                course(2, "A", "1", 1, slot(2)), course(3, "B", "1", 2, slot(2)));
        CoursePathPlanner.Result result = planner.plan(gaps("1", "1"), courses, Collections.singletonList(slot(1)));
        assertEquals(Collections.singletonList(2), result.getCourseIds());
        assertEquals(0, result.getRemainingCredits().compareTo(BigDecimal.ONE));
        assertEquals(2, result.getCandidateCount());
    }

    @Test
    void weeksAndSectionsHaveInclusiveEndpointsButDisjointWeeksCanShareAClassroomTime() {
        CoursePathPlanner.Slot first = new CoursePathPlanner.Slot(1, 1, 2, 1, 10);
        assertTrue(first.overlaps(new CoursePathPlanner.Slot(1, 2, 3, 10, 21)));
        assertFalse(first.overlaps(new CoursePathPlanner.Slot(1, 1, 2, 11, 21)));
        assertFalse(first.overlaps(new CoursePathPlanner.Slot(2, 1, 2, 1, 10)));
        assertFalse(first.overlaps(new CoursePathPlanner.Slot(1, 3, 4, 1, 10)));
    }

    @Test
    void reportsTheCourseLimitRatherThanClaimingEveryGapCanBeClosed() {
        List<CoursePathPlanner.Course> courses = new ArrayList<>();
        for (int id = 1; id <= 5; id++) courses.add(course(id, "A", "1", id, slot(id)));
        CoursePathPlanner.Result result = planner.plan(gaps("5", "0"), courses, Collections.emptyList());
        assertEquals(4, result.getCourseIds().size());
        assertEquals(0, result.getRemainingCredits().compareTo(BigDecimal.ONE));
        assertTrue(result.isSearchComplete());
    }

    @Test
    void explicitlyReportsAnExhaustedSearchBudget() {
        List<CoursePathPlanner.Course> courses = Arrays.asList(course(1, "A", "3", 0, slot(1)),
                course(2, "A", "2", 1, slot(2)));
        CoursePathPlanner.Result bounded = new CoursePathPlanner(4, 2).plan(gaps("2", "0"), courses, Collections.emptyList());
        assertEquals(Collections.singletonList(1), bounded.getCourseIds());
        assertEquals(2, bounded.getExploredStates());
        assertFalse(bounded.isSearchComplete());
        assertEquals(Collections.singletonList(2), planner.plan(gaps("2", "0"), courses, Collections.emptyList()).getCourseIds());
    }

    @Test
    void emptyPlanIsBestWhenTheRequirementsAreAlreadyMet() {
        CoursePathPlanner.Result result = planner.plan(gaps("0", "0"),
                Collections.singletonList(course(1, "A", "2", 0, slot(1))), Collections.emptyList());
        assertTrue(result.getCourseIds().isEmpty());
        assertTrue(result.isSearchComplete());
    }

    @Test
    void tiesAreStableEvenWhenInputOrderChanges() {
        List<CoursePathPlanner.Course> courses = new ArrayList<>(Arrays.asList(course(8, "A", "1", 0, slot(1)),
                course(3, "A", "1", 0, slot(2)), course(2, "B", "1", 0, slot(3))));
        for (int i = 0; i < 10; i++) {
            Collections.shuffle(courses, new Random(i));
            assertEquals(Arrays.asList(2, 3), sorted(planner.plan(gaps("1", "1"), courses, Collections.emptyList()).getCourseIds()));
        }
    }

    @Test
    void rejectsAmbiguousOrInvalidCandidateData() {
        assertThrows(IllegalArgumentException.class, () -> new CoursePathPlanner(0, 10));
        assertThrows(IllegalArgumentException.class, () -> new CoursePathPlanner.Slot(1, 3, 2, 1, 21));
        assertThrows(IllegalArgumentException.class, () -> course(1, "A", "-1", 0, slot(1)));
        assertThrows(IllegalArgumentException.class, () -> new CoursePathPlanner.Course(1, "A", BigDecimal.ONE, 0, Collections.emptyList()));
        CoursePathPlanner.Course course = course(1, "A", "1", 0, slot(1));
        assertThrows(IllegalArgumentException.class, () -> planner.plan(gaps("1", "0"), Arrays.asList(course, course), Collections.emptyList()));
    }

    @Test
    void agreesWithAnIndependentExhaustiveSubsetOracleOnOneHundredSeededCatalogs() {
        Random random = new Random(20261105);
        for (int trial = 0; trial < 100; trial++) {
            List<FixtureCourse> fixtures = new ArrayList<>();
            List<CoursePathPlanner.Course> courses = new ArrayList<>();
            int[] need = {random.nextInt(10), random.nextInt(10)};
            for (int i = 0; i < 8; i++) {
                FixtureCourse f = new FixtureCourse(i + 1, random.nextInt(2), 1 + random.nextInt(6),
                        random.nextInt(4), 1 + random.nextInt(3), 1 + random.nextInt(4));
                fixtures.add(f);
                courses.add(course(f.id, f.type == 0 ? "A" : "B", BigDecimal.valueOf(f.units, 1).toPlainString(),
                        f.rank, new CoursePathPlanner.Slot(f.day, f.section, f.section, 1, 21)));
            }
            List<Integer> expected = oracle(fixtures, need);
            CoursePathPlanner.Result actual = planner.plan(gaps(BigDecimal.valueOf(need[0], 1).toPlainString(),
                    BigDecimal.valueOf(need[1], 1).toPlainString()), courses, Collections.emptyList());
            assertEquals(expected, sorted(actual.getCourseIds()), "Catalog " + trial);
            assertTrue(actual.isSearchComplete());
        }
    }

    // Deliberately enumerates bit masks and uses integer credit units rather than the production DFS/BigDecimal implementation.
    private List<Integer> oracle(List<FixtureCourse> courses, int[] gaps) {
        List<Integer> bestIds = Collections.emptyList();
        int[] bestScore = {gaps[0] + gaps[1], 0, 0, 0};
        for (int mask = 1; mask < (1 << courses.size()); mask++) {
            if (Integer.bitCount(mask) > 4) continue;
            int[] contributed = new int[2];
            List<FixtureCourse> selected = new ArrayList<>();
            int rank = 0;
            boolean conflict = false;
            for (int i = 0; i < courses.size(); i++) {
                if ((mask & (1 << i)) == 0) continue;
                FixtureCourse course = courses.get(i);
                for (FixtureCourse earlier : selected) {
                    if (course.day == earlier.day && course.section == earlier.section) conflict = true;
                }
                selected.add(course);
                contributed[course.type] += course.units;
                rank += course.rank;
            }
            if (conflict) continue;
            int[] score = {Math.max(0, gaps[0] - contributed[0]) + Math.max(0, gaps[1] - contributed[1]),
                    Math.max(0, contributed[0] - gaps[0]) + Math.max(0, contributed[1] - gaps[1]), selected.size(), rank};
            List<Integer> ids = selected.stream().map(item -> item.id).sorted().collect(Collectors.toList());
            int compare = 0;
            for (int i = 0; i < score.length && compare == 0; i++) compare = Integer.compare(score[i], bestScore[i]);
            if (compare == 0) {
                for (int i = 0; i < ids.size() && compare == 0; i++) compare = Integer.compare(ids.get(i), bestIds.get(i));
            }
            if (compare < 0) { bestScore = score; bestIds = ids; }
        }
        return bestIds;
    }

    private static class FixtureCourse {
        private final int id, type, units, rank, day, section;
        private FixtureCourse(int id, int type, int units, int rank, int day, int section) {
            this.id = id; this.type = type; this.units = units; this.rank = rank; this.day = day; this.section = section;
        }
    }

    private static List<Integer> sorted(List<Integer> ids) { return ids.stream().sorted().collect(Collectors.toList()); }
    private static CoursePathPlanner.Course course(int id, String type, String credit, int rank, CoursePathPlanner.Slot slot) {
        return new CoursePathPlanner.Course(id, type, new BigDecimal(credit), rank, Collections.singletonList(slot));
    }
    private static CoursePathPlanner.Slot slot(int day) { return new CoursePathPlanner.Slot(day, 1, 2, 1, 21); }
    private static Map<String, BigDecimal> gaps(String a, String b) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        result.put("A", new BigDecimal(a)); result.put("B", new BigDecimal(b));
        return result;
    }
}

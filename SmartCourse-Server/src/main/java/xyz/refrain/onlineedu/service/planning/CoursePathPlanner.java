package xyz.refrain.onlineedu.service.planning;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Pure, bounded search; no database queries or enrollment side effects. */
public final class CoursePathPlanner {
    private final int maxCourses;
    private final int maxStates;

    public CoursePathPlanner(int maxCourses, int maxStates) {
        if (maxCourses < 1 || maxStates < 1) {
            throw new IllegalArgumentException("Search limits must be positive");
        }
        this.maxCourses = maxCourses;
        this.maxStates = maxStates;
    }

    public Result plan(Map<String, BigDecimal> gaps, List<Course> courses, List<Slot> occupied) {
        Map<String, BigDecimal> missing = new LinkedHashMap<>();
        gaps.forEach((type, value) -> {
            if (type == null || value == null || value.signum() < 0) {
                throw new IllegalArgumentException("Credit gaps must be known and nonnegative");
            }
            missing.put(type, value);
        });
        Set<Integer> ids = new HashSet<>();
        List<Course> eligible = new ArrayList<>();
        for (Course course : courses) {
            if (!ids.add(course.id)) {
                throw new IllegalArgumentException("Duplicate course ID: " + course.id);
            }
            if (missing.getOrDefault(course.type, BigDecimal.ZERO).signum() > 0
                    && !conflicts(occupied, course.slots)) {
                eligible.add(course);
            }
        }
        eligible.sort(Comparator.comparingInt((Course item) -> item.rank).thenComparingInt(item -> item.id));
        Search search = new Search(missing, eligible);
        search.visit(0, new ArrayList<>(), new LinkedHashMap<>(), new ArrayList<>(occupied), 0L);
        return new Result(search.best, search.bestRemaining, search.bestExcess,
                search.states, !search.truncated, eligible.size());
    }

    private final class Search {
        private final Map<String, BigDecimal> gaps;
        private final List<Course> courses;
        private List<Course> best = Collections.emptyList();
        private BigDecimal bestRemaining;
        private BigDecimal bestExcess = BigDecimal.ZERO;
        private long bestRank;
        private int states;
        private boolean truncated;

        private Search(Map<String, BigDecimal> gaps, List<Course> courses) {
            this.gaps = gaps;
            this.courses = courses;
            this.bestRemaining = gaps.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        private void visit(int start, List<Course> chosen, Map<String, BigDecimal> credits,
                           List<Slot> slots, long rank) {
            if (states >= maxStates) {
                truncated = true;
                return;
            }
            states++;
            BigDecimal remaining = BigDecimal.ZERO;
            BigDecimal excess = BigDecimal.ZERO;
            for (Map.Entry<String, BigDecimal> entry : gaps.entrySet()) {
                BigDecimal difference = entry.getValue().subtract(credits.getOrDefault(entry.getKey(), BigDecimal.ZERO));
                remaining = remaining.add(difference.max(BigDecimal.ZERO));
                excess = excess.add(difference.negate().max(BigDecimal.ZERO));
            }
            if (better(chosen, remaining, excess, rank)) {
                best = new ArrayList<>(chosen);
                bestRemaining = remaining;
                bestExcess = excess;
                bestRank = rank;
            }
            if (chosen.size() >= maxCourses || remaining.signum() == 0) {
                return;
            }
            for (int i = start; i < courses.size(); i++) {
                Course course = courses.get(i);
                BigDecimal already = credits.getOrDefault(course.type, BigDecimal.ZERO);
                // Adding to an already fulfilled bucket can only worsen excess/count.
                if (already.compareTo(gaps.get(course.type)) >= 0 || conflicts(slots, course.slots)) {
                    continue;
                }
                List<Course> nextChosen = new ArrayList<>(chosen);
                nextChosen.add(course);
                Map<String, BigDecimal> nextCredits = new LinkedHashMap<>(credits);
                nextCredits.put(course.type, already.add(course.credit));
                List<Slot> nextSlots = new ArrayList<>(slots);
                nextSlots.addAll(course.slots);
                visit(i + 1, nextChosen, nextCredits, nextSlots, rank + course.rank);
                if (truncated) {
                    return;
                }
            }
        }

        private boolean better(List<Course> chosen, BigDecimal remaining, BigDecimal excess, long rank) {
            int gapComparison = remaining.compareTo(bestRemaining);
            if (gapComparison != 0) return gapComparison < 0;
            int excessComparison = excess.compareTo(bestExcess);
            if (excessComparison != 0) return excessComparison < 0;
            if (chosen.size() != best.size()) return chosen.size() < best.size();
            if (rank != bestRank) return rank < bestRank;
            List<Integer> left = chosen.stream().map(item -> item.id).sorted().collect(Collectors.toList());
            List<Integer> right = best.stream().map(item -> item.id).sorted().collect(Collectors.toList());
            for (int i = 0; i < left.size(); i++) {
                if (!left.get(i).equals(right.get(i))) return left.get(i) < right.get(i);
            }
            return false;
        }
    }

    private static boolean conflicts(List<Slot> left, List<Slot> right) {
        for (Slot a : left) {
            for (Slot b : right) {
                if (a.overlaps(b)) return true;
            }
        }
        return false;
    }

    public static final class Slot {
        private final int day;
        private final int firstSection;
        private final int lastSection;
        private final int firstWeek;
        private final int lastWeek;

        public Slot(int day, int firstSection, int lastSection, int firstWeek, int lastWeek) {
            if (day < 1 || day > 7 || firstSection < 1 || lastSection < firstSection
                    || firstWeek < 1 || lastWeek < firstWeek) {
                throw new IllegalArgumentException("Invalid timetable interval");
            }
            this.day = day;
            this.firstSection = firstSection;
            this.lastSection = lastSection;
            this.firstWeek = firstWeek;
            this.lastWeek = lastWeek;
        }

        public boolean overlaps(Slot other) {
            return day == other.day && firstSection <= other.lastSection && other.firstSection <= lastSection
                    && firstWeek <= other.lastWeek && other.firstWeek <= lastWeek;
        }
    }

    public static final class Course {
        private final int id;
        private final String type;
        private final BigDecimal credit;
        private final int rank;
        private final List<Slot> slots;

        public Course(int id, String type, BigDecimal credit, int rank, List<Slot> slots) {
            if (id < 1 || type == null || credit == null || credit.signum() <= 0 || rank < 0
                    || slots == null || slots.isEmpty() || slots.contains(null)) {
                throw new IllegalArgumentException("A candidate needs positive credits and a known timetable");
            }
            this.id = id;
            this.type = type;
            this.credit = credit;
            this.rank = rank;
            this.slots = Collections.unmodifiableList(new ArrayList<>(slots));
        }
    }

    public static final class Result {
        private final List<Integer> courseIds;
        private final BigDecimal remainingCredits;
        private final BigDecimal excessCredits;
        private final int exploredStates;
        private final boolean searchComplete;
        private final int candidateCount;

        private Result(List<Course> courses, BigDecimal remaining, BigDecimal excess,
                       int states, boolean complete, int candidates) {
            courseIds = Collections.unmodifiableList(courses.stream().map(item -> item.id).collect(Collectors.toList()));
            remainingCredits = remaining;
            excessCredits = excess;
            exploredStates = states;
            searchComplete = complete;
            candidateCount = candidates;
        }

        public List<Integer> getCourseIds() { return courseIds; }
        public BigDecimal getRemainingCredits() { return remainingCredits; }
        public BigDecimal getExcessCredits() { return excessCredits; }
        public int getExploredStates() { return exploredStates; }
        public boolean isSearchComplete() { return searchComplete; }
        public int getCandidateCount() { return candidateCount; }
    }
}

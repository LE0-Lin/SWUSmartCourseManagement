package xyz.refrain.onlineedu.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import xyz.refrain.onlineedu.model.vo.R;
import xyz.refrain.onlineedu.utils.RUtils;
import xyz.refrain.onlineedu.service.planning.CoursePathPlanner;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SmartAdvisorService {

    private static final String DEFAULT_MAJOR = "计算机科学与技术（中外合作办学）";
    private static final double PASS_SCORE = 60.0;
    private static final int PLAN_COURSE_LIMIT = 4;
    private static final int PLAN_STATE_LIMIT = 100000;

    private final JdbcTemplate jdbcTemplate;

    public SmartAdvisorService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public R getStudentDashboard(int memberId) {
        List<Map<String, Object>> selectedCourses = querySelectedCourses(memberId);
        List<Map<String, Object>> selectedSchedules = querySchedulesForCourses(courseIds(selectedCourses));
        Map<String, Requirement> requirements = buildRequirements();
        Map<String, CreditBucket> buckets = summarizeCredits(requirements, selectedCourses);
        RecommendationSet recommendations = buildRecommendations(selectedCourses, selectedSchedules, buckets);
        boolean selectedCreditsKnown = selectedCourses.stream().allMatch(course -> {
            double credit = asDouble(course.get("credit"), Double.NaN);
            return Double.isFinite(credit) && credit >= 0 && buckets.containsKey(asString(course.get("courseType"), ""));
        });
        PlanningOutcome planning = buildPathPlan(recommendations, buckets, selectedSchedules,
                courseIds(selectedCourses), selectedCreditsKnown);
        List<Map<String, Object>> pathPlan = planning.path;

        double completedCredits = buckets.values().stream().mapToDouble(CreditBucket::getCompletedCredits).sum();
        double projectedCredits = buckets.values().stream().mapToDouble(CreditBucket::getProjectedCredits).sum();
        double requiredCredits = requirements.values().stream().mapToDouble(item -> item.requiredCredits).sum();
        double fulfilledCompletedCredits = buckets.values().stream()
                .mapToDouble(item -> Math.min(item.getCompletedCredits(), item.getRequiredCredits()))
                .sum();
        double fulfilledProjectedCredits = buckets.values().stream()
                .mapToDouble(item -> Math.min(item.getProjectedCredits(), item.getRequiredCredits()))
                .sum();
        double completionRate = requiredCredits == 0 ? 0 : Math.min(100, fulfilledCompletedCredits * 100 / requiredCredits);
        double projectedRate = requiredCredits == 0 ? 0 : Math.min(100, fulfilledProjectedCredits * 100 / requiredCredits);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("majorName", DEFAULT_MAJOR);
        result.put("engineName", "培养方案学分分析与课表约束规划");
        result.put("summary", buildSummary(completedCredits, projectedCredits, requiredCredits, completionRate, projectedRate, buckets));
        result.put("requirements", new ArrayList<>(buckets.values()));
        result.put("selectedCourses", selectedCourses);
        result.put("recommendations", visibleRecommendations(recommendations.ranked));
        result.put("pathPlan", pathPlan);
        result.put("planning", planning.report);
        List<Map<String, Object>> notes = buildAdvisorNotes(buckets, pathPlan);
        Map<String, Object> planningNote = new LinkedHashMap<>();
        planningNote.put("level", "COMPLETE".equals(planning.report.get("status")) ? "info" : "warning");
        planningNote.put("text", planning.report.get("message"));
        notes.add(planningNote);
        result.put("advisorNotes", notes);
        return RUtils.success("智能毕业路径分析", result);
    }

    @SuppressWarnings("unchecked")
    public R getAdminOverview() {
        List<Map<String, Object>> members = jdbcTemplate.queryForList(
                "SELECT id AS memberId, nickname, mobile, email FROM uctr_member WHERE enable = 1 ORDER BY mobile ASC"
        );
        List<Map<String, Object>> riskRows = new ArrayList<>();
        int high = 0;
        int medium = 0;
        int low = 0;
        for (Map<String, Object> member : members) {
            R dashboard = getStudentDashboard(asInt(member.get("memberId")));
            Map<String, Object> data = (Map<String, Object>) dashboard.getData();
            Map<String, Object> summary = (Map<String, Object>) data.get("summary");
            String level = asString(summary.get("riskLevel"), "MEDIUM");
            if ("HIGH".equals(level)) {
                high++;
            } else if ("LOW".equals(level)) {
                low++;
            } else {
                medium++;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("memberId", member.get("memberId"));
            row.put("nickname", member.get("nickname"));
            row.put("mobile", member.get("mobile"));
            row.put("riskLevel", level);
            row.put("riskText", summary.get("riskText"));
            row.put("completionRate", summary.get("completionRate"));
            row.put("remainingCredits", summary.get("projectedRemainingCredits"));
            row.put("advisorNotes", data.get("advisorNotes"));
            riskRows.add(row);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("studentCount", members.size());
        result.put("highRiskCount", high);
        result.put("mediumRiskCount", medium);
        result.put("lowRiskCount", low);
        result.put("riskRows", riskRows);
        return RUtils.success("智能毕业风险总览", result);
    }

    public R diagnoseCourse(int memberId, int courseId) {
        List<Map<String, Object>> selectedCourses = querySelectedCourses(memberId);
        if (selectedCourses.stream().anyMatch(item -> asInt(item.get("courseId")) == courseId)) {
            return RUtils.success("选课诊断", diagnosis(false, "DUPLICATED", "该课程已经在你的课表中。"));
        }
        Map<String, Object> course = queryCourse(courseId);
        if (course == null) {
            return RUtils.success("选课诊断", diagnosis(false, "NOT_FOUND", "课程不存在或暂不可选。"));
        }
        double targetCredit = asDouble(course.get("credit"), Double.NaN);
        if (!Double.isFinite(targetCredit) || targetCredit <= 0) {
            return RUtils.success("选课诊断", diagnosis(false, "CREDIT_UNKNOWN", "课程学分缺失或无效，请先核对课程信息。"));
        }
        List<Map<String, Object>> selectedSchedules = querySchedulesForCourses(courseIds(selectedCourses));
        List<Map<String, Object>> targetSchedules = querySchedulesForCourses(new LinkedHashSet<>(Collections.singletonList(courseId)));
        Set<Integer> scheduledIds = courseIds(selectedSchedules);
        if (!scheduledIds.containsAll(courseIds(selectedCourses)) || toSlots(selectedSchedules) == null
                || toSlots(targetSchedules) == null || targetSchedules.isEmpty()) {
            return RUtils.success("选课诊断", diagnosis(false, "SCHEDULE_UNKNOWN",
                    "部分课程的课表缺失或无效，暂时无法确认时间是否冲突，请先补齐排课信息。"));
        }
        Map<String, Object> conflict = findFirstConflict(selectedSchedules, targetSchedules);
        if (conflict != null) {
            return RUtils.success("选课诊断", diagnosis(false, "TIME_CONFLICT",
                    "该课程与《" + conflict.get("selectedTitle") + "》存在上课时间冲突。"));
        }
        Map<String, Requirement> requirements = buildRequirements();
        Map<String, CreditBucket> buckets = summarizeCredits(requirements, selectedCourses);
        String type = asString(course.get("courseType"), "MAJOR_ELECTIVE");
        CreditBucket bucket = buckets.get(type);
        String label = bucket == null ? "选修" : bucket.getTypeLabel();
        double remaining = bucket == null ? 0 : bucket.getRemainingAfterCurrent();
        String reason = remaining > 0
                ? "该课程可补充" + label + "学分缺口，且与当前已选课表不冲突。若同时选择多门课，请以候选补选方案为准，系统会继续排除推荐课程之间的冲突。"
                : "该课程与当前已选课表不冲突，可作为能力拓展课程。若同时选择多门课，请以候选补选方案为准，系统会继续排除推荐课程之间的冲突。";
        return RUtils.success("选课诊断", diagnosis(true, "OK", reason));
    }

    private Map<String, Object> buildSummary(double completedCredits, double projectedCredits, double requiredCredits,
                                             double completionRate, double projectedRate,
                                             Map<String, CreditBucket> buckets) {
        double totalRemaining = buckets.values().stream().mapToDouble(CreditBucket::getRemainingCredits).sum();
        double projectedRemaining = buckets.values().stream().mapToDouble(CreditBucket::getRemainingAfterCurrent).sum();
        boolean hasRequiredGap = buckets.values().stream()
                .anyMatch(item -> item.getTypeCode().contains("REQUIRED") && item.getRemainingAfterCurrent() > 0);
        String riskLevel;
        String riskText;
        if (projectedRemaining <= 0) {
            riskLevel = "LOW";
            riskText = "预计满足毕业学分要求";
        } else if (hasRequiredGap || projectedRemaining >= 8) {
            riskLevel = "HIGH";
            riskText = "存在明显毕业风险";
        } else {
            riskLevel = "MEDIUM";
            riskText = "存在轻度学分缺口";
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("requiredCredits", requiredCredits);
        summary.put("completedCredits", completedCredits);
        summary.put("projectedCredits", projectedCredits);
        summary.put("remainingCredits", totalRemaining);
        summary.put("projectedRemainingCredits", projectedRemaining);
        summary.put("completionRate", round(completionRate));
        summary.put("projectedRate", round(projectedRate));
        summary.put("riskLevel", riskLevel);
        summary.put("riskText", riskText);
        return summary;
    }

    private List<Map<String, Object>> buildAdvisorNotes(Map<String, CreditBucket> buckets, List<Map<String, Object>> pathPlan) {
        List<Map<String, Object>> notes = new ArrayList<>();
        for (CreditBucket bucket : buckets.values()) {
            if (bucket.getRemainingAfterCurrent() > 0) {
                Map<String, Object> note = new LinkedHashMap<>();
                note.put("level", bucket.getTypeCode().contains("REQUIRED") ? "danger" : "warning");
                note.put("text", bucket.getTypeLabel() + "仍缺 " + formatCredit(bucket.getRemainingAfterCurrent())
                        + " 学分，系统会优先推荐同类且无课表冲突的课程。");
                notes.add(note);
            }
        }
        if (!pathPlan.isEmpty()) {
            Map<String, Object> note = new LinkedHashMap<>();
            note.put("level", "success");
            double remainingAfterPath = remainingAfterPathPlan(buckets, pathPlan);
            note.put("text", remainingAfterPath <= 0
                    ? "已生成一套本学期补齐路径，可作为 what-if 选课模拟方案。"
                    : "已生成一套本学期候选补选路径，但仍可能剩余 " + formatCredit(remainingAfterPath)
                    + " 学分缺口，需要继续关注无冲突课程。");
            notes.add(note);
        }
        if (notes.isEmpty()) {
            Map<String, Object> note = new LinkedHashMap<>();
            note.put("level", "success");
            note.put("text", "当前培养方案完成度良好，建议保持课程节奏并关注成绩录入。");
            notes.add(note);
        }
        return notes;
    }

    private double remainingAfterPathPlan(Map<String, CreditBucket> buckets, List<Map<String, Object>> pathPlan) {
        Map<String, Double> missing = new HashMap<>();
        for (CreditBucket bucket : buckets.values()) {
            missing.put(bucket.getTypeCode(), bucket.getRemainingAfterCurrent());
        }
        for (Map<String, Object> course : pathPlan) {
            String type = asString(course.get("courseType"), "MAJOR_ELECTIVE");
            double need = missing.getOrDefault(type, 0.0);
            if (need > 0) {
                missing.put(type, Math.max(0, need - asDouble(course.get("credit"), 2)));
            }
        }
        return missing.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    private RecommendationSet buildRecommendations(List<Map<String, Object>> selectedCourses,
                                                           List<Map<String, Object>> selectedSchedules,
                                                           Map<String, CreditBucket> buckets) {
        Set<Integer> selectedCourseIds = courseIds(selectedCourses);
        List<Map<String, Object>> candidates = jdbcTemplate.queryForList(
                "SELECT c.id AS courseId, c.title, c.credit, c.course_type AS courseType, c.major_name AS majorName, "
                        + "c.buy_count AS selectCount, c.view_count AS viewCount, t.name AS teacherName "
                        + "FROM edu_course c LEFT JOIN edu_teacher t ON c.teacher_id = t.id "
                        + "WHERE c.enable = 1 AND c.status = 1 "
                        + (selectedCourseIds.isEmpty() ? "" : "AND c.id NOT IN (" + placeholders(selectedCourseIds.size()) + ") ")
                        + "ORDER BY c.buy_count DESC, c.view_count DESC, c.id ASC",
                selectedCourseIds.toArray()
        );
        Map<Integer, List<Map<String, Object>>> schedulesByCourse = querySchedulesForCourses(courseIds(candidates))
                .stream().collect(Collectors.groupingBy(item -> asInt(item.get("courseId"))));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> candidate : candidates) {
            int courseId = asInt(candidate.get("courseId"));
            List<Map<String, Object>> targetSchedules = schedulesByCourse.getOrDefault(courseId, Collections.emptyList());
            Map<String, Object> conflict = findFirstConflict(selectedSchedules, targetSchedules);
            String type = asString(candidate.get("courseType"), "MAJOR_ELECTIVE");
            CreditBucket bucket = buckets.get(type);
            double remaining = bucket == null ? 0 : bucket.getRemainingAfterCurrent();
            double credit = asDouble(candidate.get("credit"), 0);
            boolean validCredit = Double.isFinite(credit) && credit > 0;
            boolean scheduleKnown = !targetSchedules.isEmpty() && toSlots(targetSchedules) != null;
            int rawScore = (int) Math.round(Math.max(0, remaining) * 15 + credit * 8
                    + asInt(candidate.get("selectCount")) * 2 + asInt(candidate.get("viewCount")));
            boolean selectable = conflict == null && scheduleKnown && validCredit;
            int rankScore = rawScore + (remaining > 0 ? 10000 : 0) - (selectable ? 0 : 7500);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("courseId", courseId);
            item.put("title", candidate.get("title"));
            item.put("courseType", type);
            item.put("majorName", candidate.get("majorName"));
            item.put("selectCount", candidate.get("selectCount"));
            item.put("viewCount", candidate.get("viewCount"));
            item.put("teacherName", candidate.get("teacherName"));
            item.put("typeLabel", courseTypeLabel(type));
            item.put("credit", credit);
            item.put("selectable", selectable);
            item.put("conflict", conflict != null);
            item.put("scheduleKnown", scheduleKnown);
            item.put("conflictTitle", conflict == null ? "" : conflict.get("selectedTitle"));
            item.put("conflictText", conflict == null ? "" : "与《" + conflict.get("selectedTitle") + "》上课时间冲突");
            item.put("fitScore", selectable ? Math.min(100, Math.max(55, rawScore)) : Math.min(72, Math.max(45, rawScore / 2)));
            item.put("reason", !scheduleKnown ? "课表时间缺失或无效，补齐排课后才能确认是否可选。"
                    : !validCredit ? "课程学分缺失或无效，暂不纳入补选方案。"
                    : buildRecommendationReason(courseTypeLabel(type), credit, remaining, conflict));
            item.put("scheduleText", scheduleText(targetSchedules));
            item.put("rankScore", rankScore);
            result.add(item);
        }
        List<Map<String, Object>> ranked = result.stream()
                .sorted((left, right) -> Integer.compare(asInt(right.get("rankScore")), asInt(left.get("rankScore"))))
                .collect(Collectors.toList());
        ranked.forEach(item -> item.remove("rankScore"));
        return new RecommendationSet(ranked, schedulesByCourse);
    }

    private List<Map<String, Object>> visibleRecommendations(List<Map<String, Object>> ranked) {
        List<Map<String, Object>> visible = ranked.stream().limit(12).collect(Collectors.toList());
        boolean hasVisibleConflict = visible.stream().anyMatch(item -> Boolean.TRUE.equals(item.get("conflict")));
        if (!hasVisibleConflict) {
            for (Map<String, Object> item : ranked) {
                if (Boolean.TRUE.equals(item.get("conflict"))) {
                    if (visible.size() >= 12) {
                        visible.set(visible.size() - 1, item);
                    } else {
                        visible.add(item);
                    }
                    break;
                }
            }
        }
        return visible;
    }

    private String buildRecommendationReason(String typeLabel, double credit, double remaining, Map<String, Object> conflict) {
        String base = remaining > 0
                ? "可补齐" + typeLabel + "缺口，预计贡献 " + formatCredit(credit) + " 学分"
                : "当前学分缺口不明显，可作为能力拓展课程";
        if (conflict == null) {
            return base + "，且与当前已选课表不冲突。";
        }
        return base + "，但与《" + conflict.get("selectedTitle") + "》冲突，暂不建议本轮选择。";
    }

    private PlanningOutcome buildPathPlan(RecommendationSet recommendations,
                                                    Map<String, CreditBucket> buckets,
                                                    List<Map<String, Object>> selectedSchedules,
                                                    Set<Integer> selectedCourseIds,
                                                    boolean selectedCreditsKnown) {
        Map<String, BigDecimal> missing = new LinkedHashMap<>();
        for (CreditBucket bucket : buckets.values()) {
            missing.put(bucket.getTypeCode(), BigDecimal.valueOf(bucket.getRemainingAfterCurrent()));
        }
        List<CoursePathPlanner.Slot> occupied = toSlots(selectedSchedules);
        boolean selectedSchedulesKnown = occupied != null && courseIds(selectedSchedules).containsAll(selectedCourseIds);
        boolean canSearch = selectedSchedulesKnown && selectedCreditsKnown;
        List<CoursePathPlanner.Course> candidates = new ArrayList<>();
        Map<Integer, Map<String, Object>> byId = new LinkedHashMap<>();
        List<Integer> excludedIds = new ArrayList<>();
        int rank = 0;
        for (Map<String, Object> course : recommendations.ranked) {
            int courseId = asInt(course.get("courseId"));
            String type = asString(course.get("courseType"), "MAJOR_ELECTIVE");
            byId.put(courseId, course);
            if (missing.getOrDefault(type, BigDecimal.ZERO).signum() <= 0) continue;
            double credit = asDouble(course.get("credit"), 0);
            if (!Boolean.TRUE.equals(course.get("scheduleKnown")) || !Double.isFinite(credit) || credit <= 0) {
                excludedIds.add(courseId);
                continue;
            }
            if (!Boolean.TRUE.equals(course.get("selectable"))) continue;
            candidates.add(new CoursePathPlanner.Course(courseId, type, BigDecimal.valueOf(credit), rank++,
                    toSlots(recommendations.schedules.get(courseId))));
        }
        CoursePathPlanner.Result best = new CoursePathPlanner(PLAN_COURSE_LIMIT, PLAN_STATE_LIMIT).plan(missing,
                canSearch ? candidates : Collections.emptyList(),
                canSearch ? occupied : Collections.emptyList());

        List<Map<String, Object>> path = new ArrayList<>();
        Map<String, BigDecimal> remaining = new LinkedHashMap<>(missing);
        for (Integer id : best.getCourseIds()) {
            Map<String, Object> course = byId.get(id);
            String type = asString(course.get("courseType"), "MAJOR_ELECTIVE");
            BigDecimal need = remaining.get(type);
            BigDecimal credit = BigDecimal.valueOf(asDouble(course.get("credit"), 0));
            Map<String, Object> step = new LinkedHashMap<>(course);
            step.put("afterSelectGap", need.subtract(credit).max(BigDecimal.ZERO));
            path.add(step);
            remaining.put(type, need.subtract(credit));
        }
        Map<String, Object> report = new LinkedHashMap<>();
        String status = !selectedCreditsKnown ? "INSUFFICIENT_CREDITS" : !selectedSchedulesKnown ? "INSUFFICIENT_SCHEDULES"
                : !best.isSearchComplete() ? "SEARCH_LIMIT" : !excludedIds.isEmpty() ? "INCOMPLETE_DATA" : "COMPLETE";
        report.put("status", status);
        report.put("searchComplete", canSearch && best.isSearchComplete());
        report.put("inputComplete", canSearch && excludedIds.isEmpty());
        report.put("candidateCount", best.getCandidateCount());
        report.put("excludedCourseIds", excludedIds);
        report.put("maxCourses", PLAN_COURSE_LIMIT);
        report.put("stateLimit", PLAN_STATE_LIMIT);
        report.put("exploredStates", canSearch ? best.getExploredStates() : 0);
        report.put("remainingCredits", best.getRemainingCredits());
        report.put("excessCredits", best.getExcessCredits());
        report.put("message", !selectedCreditsKnown ? "已选课程的学分或分类信息缺失或无效，请先核对数据后再生成方案。"
                : !selectedSchedulesKnown ? "已选课程的课表缺失或无效，暂不生成可能冲突的补选方案。"
                : "搜索 " + best.getCandidateCount() + " 门候选课程，每套最多 " + PLAN_COURSE_LIMIT + " 门；"
                + (!best.isSearchComplete() ? "已达到搜索上限，当前结果未保证最优；" : "已完成该范围内的搜索；")
                + (!excludedIds.isEmpty() ? "另有 " + excludedIds.size() + " 门课因课表或学分信息不完整被排除；" : "")
                + "方案后仍缺 " + formatCredit(best.getRemainingCredits().doubleValue()) + " 学分。");
        return new PlanningOutcome(path, report);
    }

    private List<CoursePathPlanner.Slot> toSlots(List<Map<String, Object>> schedules) {
        List<CoursePathPlanner.Slot> result = new ArrayList<>();
        try {
            for (Map<String, Object> item : schedules) {
                result.add(new CoursePathPlanner.Slot(asInt(item.get("dayOfWeek")),
                        asInt(item.get("sectionStart")), asInt(item.get("sectionEnd")),
                        asInt(item.get("startWeek")), asInt(item.get("endWeek"))));
            }
        } catch (IllegalArgumentException exception) {
            return null;
        }
        return result;
    }

    private Map<String, CreditBucket> summarizeCredits(Map<String, Requirement> requirements,
                                                       List<Map<String, Object>> selectedCourses) {
        Map<String, CreditBucket> buckets = new LinkedHashMap<>();
        requirements.forEach((type, requirement) -> buckets.put(type, new CreditBucket(requirement)));
        for (Map<String, Object> course : selectedCourses) {
            String type = asString(course.get("courseType"), "");
            CreditBucket bucket = buckets.get(type);
            if (bucket == null) {
                continue;
            }
            double credit = asDouble(course.get("credit"), 0);
            if (!Double.isFinite(credit) || credit < 0) credit = 0;
            Double score = asNullableDouble(course.get("score"));
            bucket.selectedCredits += credit;
            if (score != null && score >= PASS_SCORE) {
                bucket.completedCredits += credit;
            } else if (score == null) {
                bucket.pendingCredits += credit;
            } else {
                bucket.failedCredits += credit;
            }
        }
        return buckets;
    }

    private List<Map<String, Object>> querySelectedCourses(int memberId) {
        return jdbcTemplate.queryForList(
                "SELECT c.id AS courseId, c.title, c.credit, c.course_type AS courseType, c.major_name AS majorName, "
                        + "c.buy_count AS selectCount, c.view_count AS viewCount, g.score "
                        + "FROM rel_course_member r "
                        + "JOIN edu_course c ON r.course_id = c.id "
                        + "LEFT JOIN edu_grade g ON g.course_id = c.id AND g.member_id = r.member_id "
                        + "WHERE r.member_id = ? ORDER BY c.id",
                memberId
        );
    }

    private Map<String, Object> queryCourse(int courseId) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT id AS courseId, title, credit, course_type AS courseType, major_name AS majorName "
                        + "FROM edu_course WHERE id = ? AND enable = 1 AND status = 1",
                courseId
        );
        return list.isEmpty() ? null : list.get(0);
    }

    private List<Map<String, Object>> querySchedulesForCourses(Set<Integer> courseIds) {
        if (CollectionUtils.isEmpty(courseIds)) {
            return Collections.emptyList();
        }
        return jdbcTemplate.queryForList(
                "SELECT s.course_id AS courseId, c.title, s.day_of_week AS dayOfWeek, "
                        + "s.section_start AS sectionStart, s.section_end AS sectionEnd, "
                        + "s.start_week AS startWeek, s.end_week AS endWeek, s.location "
                        + "FROM edu_course_schedule s JOIN edu_course c ON s.course_id = c.id "
                        + "WHERE s.course_id IN (" + placeholders(courseIds.size()) + ")",
                courseIds.toArray()
        );
    }

    private Map<String, Requirement> buildRequirements() {
        Map<String, Requirement> map = new LinkedHashMap<>();
        map.put("PUBLIC_REQUIRED", new Requirement("PUBLIC_REQUIRED", "通识教育必修", 37));
        map.put("GENERAL_ELECTIVE", new Requirement("GENERAL_ELECTIVE", "通识教育选修", 8));
        map.put("DISCIPLINE_REQUIRED", new Requirement("DISCIPLINE_REQUIRED", "学科基础", 33.5));
        map.put("MAJOR_REQUIRED", new Requirement("MAJOR_REQUIRED", "专业发展必修", 32.5));
        map.put("MAJOR_ELECTIVE", new Requirement("MAJOR_ELECTIVE", "专业发展选修", 17));
        map.put("PRACTICE_REQUIRED", new Requirement("PRACTICE_REQUIRED", "综合实践", 32));
        return map;
    }

    private Map<String, Object> findFirstConflict(List<Map<String, Object>> selectedSchedules,
                                                  List<Map<String, Object>> targetSchedules) {
        for (Map<String, Object> selected : selectedSchedules) {
            for (Map<String, Object> target : targetSchedules) {
                if (isScheduleConflict(selected, target)) {
                    Map<String, Object> conflict = new LinkedHashMap<>();
                    conflict.put("selectedCourseId", selected.get("courseId"));
                    conflict.put("selectedTitle", selected.get("title"));
                    conflict.put("targetCourseId", target.get("courseId"));
                    conflict.put("targetTitle", target.get("title"));
                    return conflict;
                }
            }
        }
        return null;
    }

    private boolean isScheduleConflict(Map<String, Object> left, Map<String, Object> right) {
        if (asInt(left.get("dayOfWeek")) != asInt(right.get("dayOfWeek"))) {
            return false;
        }
        boolean weekOverlap = asInt(left.get("startWeek"), 1) <= asInt(right.get("endWeek"), 21)
                && asInt(right.get("startWeek"), 1) <= asInt(left.get("endWeek"), 21);
        boolean sectionOverlap = asInt(left.get("sectionStart")) <= asInt(right.get("sectionEnd"))
                && asInt(right.get("sectionStart")) <= asInt(left.get("sectionEnd"));
        return weekOverlap && sectionOverlap;
    }

    private Map<String, Object> diagnosis(boolean selectable, String code, String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("selectable", selectable);
        result.put("code", code);
        result.put("message", message);
        return result;
    }

    private String scheduleText(List<Map<String, Object>> schedules) {
        if (CollectionUtils.isEmpty(schedules)) {
            return "时间待定";
        }
        return schedules.stream().map(item -> "周" + weekday(asInt(item.get("dayOfWeek")))
                + " 第" + item.get("sectionStart") + "-" + item.get("sectionEnd") + "节"
                + " / " + asInt(item.get("startWeek"), 1) + "-" + asInt(item.get("endWeek"), 21) + "周"
                + (item.get("location") == null ? "" : " / " + item.get("location")))
                .collect(Collectors.joining("；"));
    }

    private Set<Integer> courseIds(List<Map<String, Object>> courses) {
        return courses.stream()
                .map(item -> asInt(item.get("courseId")))
                .filter(id -> id > 0)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String placeholders(int size) {
        return String.join(",", Collections.nCopies(size, "?"));
    }

    private String courseTypeLabel(String type) {
        switch (type) {
            case "PUBLIC_REQUIRED":
                return "通识教育必修";
            case "GENERAL_ELECTIVE":
                return "通识教育选修";
            case "DISCIPLINE_REQUIRED":
                return "学科基础";
            case "MAJOR_REQUIRED":
                return "专业发展必修";
            case "PRACTICE_REQUIRED":
                return "综合实践";
            case "MAJOR_ELECTIVE":
            default:
                return "专业发展选修";
        }
    }

    private String weekday(int day) {
        List<String> names = Arrays.asList("", "一", "二", "三", "四", "五", "六", "日");
        return day >= 1 && day <= 7 ? names.get(day) : "?";
    }

    private int asInt(Object value) {
        return asInt(value, 0);
    }

    private int asInt(Object value, int defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private double asDouble(Object value, double defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Double asNullableDouble(Object value) {
        if (value == null) {
            return null;
        }
        return asDouble(value, 0);
    }

    private String asString(Object value, String defaultValue) {
        return value == null ? defaultValue : value.toString();
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private String formatCredit(double value) {
        return value == Math.floor(value) ? String.valueOf((int) value) : String.valueOf(round(value));
    }

    private static class Requirement {
        private final String typeCode;
        private final String typeLabel;
        private final double requiredCredits;

        private Requirement(String typeCode, String typeLabel, double requiredCredits) {
            this.typeCode = typeCode;
            this.typeLabel = typeLabel;
            this.requiredCredits = requiredCredits;
        }
    }

    private static class RecommendationSet {
        private final List<Map<String, Object>> ranked;
        private final Map<Integer, List<Map<String, Object>>> schedules;

        private RecommendationSet(List<Map<String, Object>> ranked, Map<Integer, List<Map<String, Object>>> schedules) {
            this.ranked = ranked;
            this.schedules = schedules;
        }
    }

    private static class PlanningOutcome {
        private final List<Map<String, Object>> path;
        private final Map<String, Object> report;

        private PlanningOutcome(List<Map<String, Object>> path, Map<String, Object> report) {
            this.path = path;
            this.report = report;
        }
    }

    public static class CreditBucket {
        private final String typeCode;
        private final String typeLabel;
        private final double requiredCredits;
        private double selectedCredits;
        private double completedCredits;
        private double pendingCredits;
        private double failedCredits;

        private CreditBucket(Requirement requirement) {
            this.typeCode = requirement.typeCode;
            this.typeLabel = requirement.typeLabel;
            this.requiredCredits = requirement.requiredCredits;
        }

        public String getTypeCode() {
            return typeCode;
        }

        public String getTypeLabel() {
            return typeLabel;
        }

        public double getRequiredCredits() {
            return requiredCredits;
        }

        public double getSelectedCredits() {
            return selectedCredits;
        }

        public double getCompletedCredits() {
            return completedCredits;
        }

        public double getPendingCredits() {
            return pendingCredits;
        }

        public double getFailedCredits() {
            return failedCredits;
        }

        public double getProjectedCredits() {
            return completedCredits + pendingCredits;
        }

        public double getRemainingCredits() {
            return Math.max(0, requiredCredits - completedCredits);
        }

        public double getRemainingAfterCurrent() {
            return Math.max(0, requiredCredits - getProjectedCredits());
        }

        public double getProgress() {
            return requiredCredits == 0 ? 0 : Math.min(100, Math.round(completedCredits * 1000 / requiredCredits) / 10.0);
        }

        public double getProjectedProgress() {
            return requiredCredits == 0 ? 0 : Math.min(100, Math.round(getProjectedCredits() * 1000 / requiredCredits) / 10.0);
        }
    }
}

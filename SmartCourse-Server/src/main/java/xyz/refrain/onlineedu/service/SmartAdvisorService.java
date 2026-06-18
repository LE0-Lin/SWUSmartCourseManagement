package xyz.refrain.onlineedu.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import xyz.refrain.onlineedu.model.vo.R;
import xyz.refrain.onlineedu.utils.RUtils;

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

    private final JdbcTemplate jdbcTemplate;

    public SmartAdvisorService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public R getStudentDashboard(int memberId) {
        List<Map<String, Object>> selectedCourses = querySelectedCourses(memberId);
        List<Map<String, Object>> selectedSchedules = querySchedulesForCourses(courseIds(selectedCourses));
        Map<String, Requirement> requirements = buildRequirements();
        Map<String, CreditBucket> buckets = summarizeCredits(requirements, selectedCourses);
        List<Map<String, Object>> recommendations = buildRecommendations(selectedCourses, selectedSchedules, buckets);
        List<Map<String, Object>> pathPlan = buildPathPlan(recommendations, buckets, selectedSchedules);

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
        result.put("engineName", "培养方案数字孪生 + 可解释选课推理引擎");
        result.put("summary", buildSummary(completedCredits, projectedCredits, requiredCredits, completionRate, projectedRate, buckets));
        result.put("requirements", new ArrayList<>(buckets.values()));
        result.put("selectedCourses", selectedCourses);
        result.put("recommendations", recommendations);
        result.put("pathPlan", pathPlan);
        result.put("advisorNotes", buildAdvisorNotes(buckets, pathPlan));
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
        List<Map<String, Object>> selectedSchedules = querySchedulesForCourses(courseIds(selectedCourses));
        List<Map<String, Object>> targetSchedules = querySchedulesForCourses(new LinkedHashSet<>(Collections.singletonList(courseId)));
        Map<String, Object> conflict = findFirstConflict(selectedSchedules, targetSchedules);
        if (conflict != null) {
            return RUtils.success("选课诊断", diagnosis(false, "TIME_CONFLICT",
                    "该课程与《" + conflict.get("selectedTitle") + "》存在上课时间冲突。"));
        }
        Map<String, Object> course = queryCourse(courseId);
        if (course == null) {
            return RUtils.success("选课诊断", diagnosis(false, "NOT_FOUND", "课程不存在或暂不可选。"));
        }
        Map<String, Requirement> requirements = buildRequirements();
        Map<String, CreditBucket> buckets = summarizeCredits(requirements, selectedCourses);
        String type = asString(course.get("courseType"), "MAJOR_ELECTIVE");
        CreditBucket bucket = buckets.get(type);
        String label = bucket == null ? "选修" : bucket.getTypeLabel();
        double remaining = bucket == null ? 0 : bucket.getRemainingAfterCurrent();
        String reason = remaining > 0
                ? "该课程可补充" + label + "学分缺口，且与当前已选课表不冲突。若同时选择多门课，请以一键补齐方案为准，系统会继续排除推荐课程之间的冲突。"
                : "该课程与当前已选课表不冲突，可作为能力拓展课程。若同时选择多门课，请以一键补齐方案为准，系统会继续排除推荐课程之间的冲突。";
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

    private List<Map<String, Object>> buildRecommendations(List<Map<String, Object>> selectedCourses,
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
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> candidate : candidates) {
            int courseId = asInt(candidate.get("courseId"));
            List<Map<String, Object>> targetSchedules = querySchedulesForCourses(new LinkedHashSet<>(Collections.singletonList(courseId)));
            Map<String, Object> conflict = findFirstConflict(selectedSchedules, targetSchedules);
            String type = asString(candidate.get("courseType"), "MAJOR_ELECTIVE");
            CreditBucket bucket = buckets.get(type);
            double remaining = bucket == null ? 0 : bucket.getRemainingAfterCurrent();
            double credit = asDouble(candidate.get("credit"), 2);
            int rawScore = (int) Math.round(Math.max(0, remaining) * 15 + credit * 8
                    + asInt(candidate.get("selectCount")) * 2 + asInt(candidate.get("viewCount")));
            boolean selectable = conflict == null;
            int rankScore = rawScore + (remaining > 0 ? 10000 : 0) - (selectable ? 0 : 7500);
            Map<String, Object> item = new LinkedHashMap<>(candidate);
            item.put("typeLabel", courseTypeLabel(type));
            item.put("credit", credit);
            item.put("selectable", selectable);
            item.put("conflict", !selectable);
            item.put("conflictTitle", conflict == null ? "" : conflict.get("selectedTitle"));
            item.put("conflictText", conflict == null ? "" : "与《" + conflict.get("selectedTitle") + "》上课时间冲突");
            item.put("fitScore", selectable ? Math.min(100, Math.max(55, rawScore)) : Math.min(72, Math.max(45, rawScore / 2)));
            item.put("reason", buildRecommendationReason(courseTypeLabel(type), credit, remaining, conflict));
            item.put("scheduleText", scheduleText(targetSchedules));
            item.put("rankScore", rankScore);
            result.add(item);
        }
        List<Map<String, Object>> ranked = result.stream()
                .sorted((left, right) -> Integer.compare(asInt(right.get("rankScore")), asInt(left.get("rankScore"))))
                .collect(Collectors.toList());
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
        ranked.forEach(item -> item.remove("rankScore"));
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

    private List<Map<String, Object>> buildPathPlan(List<Map<String, Object>> recommendations,
                                                    Map<String, CreditBucket> buckets,
                                                    List<Map<String, Object>> selectedSchedules) {
        Map<String, Double> missing = new HashMap<>();
        for (CreditBucket bucket : buckets.values()) {
            missing.put(bucket.getTypeCode(), bucket.getRemainingAfterCurrent());
        }
        List<Map<String, Object>> candidates = recommendations.stream()
                .filter(course -> Boolean.TRUE.equals(course.get("selectable")))
                .filter(course -> missing.getOrDefault(asString(course.get("courseType"), "MAJOR_ELECTIVE"), 0.0) > 0)
                .collect(Collectors.toList());
        PathSelection best = new PathSelection();
        searchPathPlan(candidates, 0, missing, new HashMap<>(), new ArrayList<>(),
                new ArrayList<>(selectedSchedules), new HashMap<>(), 0, best);

        List<Map<String, Object>> path = new ArrayList<>();
        Map<String, Double> remaining = new HashMap<>(missing);
        for (Map<String, Object> course : best.courses) {
            String type = asString(course.get("courseType"), "MAJOR_ELECTIVE");
            double need = remaining.getOrDefault(type, 0.0);
            double credit = asDouble(course.get("credit"), 2);
            Map<String, Object> step = new LinkedHashMap<>(course);
            step.put("afterSelectGap", Math.max(0, need - credit));
            path.add(step);
            remaining.put(type, need - credit);
        }
        return path;
    }

    private void searchPathPlan(List<Map<String, Object>> candidates,
                                int start,
                                Map<String, Double> missing,
                                Map<String, Double> contributions,
                                List<Map<String, Object>> chosen,
                                List<Map<String, Object>> plannedSchedules,
                                Map<Integer, List<Map<String, Object>>> scheduleCache,
                                int rankTotal,
                                PathSelection best) {
        if (!chosen.isEmpty()) {
            best.consider(chosen, missing, contributions, rankTotal);
        }
        if (chosen.size() >= 4) {
            return;
        }
        for (int index = start; index < candidates.size(); index++) {
            Map<String, Object> course = candidates.get(index);
            int courseId = asInt(course.get("courseId"));
            List<Map<String, Object>> schedules = scheduleCache.computeIfAbsent(courseId,
                    id -> querySchedulesForCourses(new LinkedHashSet<>(Collections.singletonList(id))));
            if (findFirstConflict(plannedSchedules, schedules) != null) {
                continue;
            }
            String type = asString(course.get("courseType"), "MAJOR_ELECTIVE");
            Map<String, Double> nextContributions = new HashMap<>(contributions);
            nextContributions.put(type, nextContributions.getOrDefault(type, 0.0)
                    + asDouble(course.get("credit"), 2));
            List<Map<String, Object>> nextChosen = new ArrayList<>(chosen);
            nextChosen.add(course);
            List<Map<String, Object>> nextSchedules = new ArrayList<>(plannedSchedules);
            nextSchedules.addAll(schedules);
            searchPathPlan(candidates, index + 1, missing, nextContributions, nextChosen,
                    nextSchedules, scheduleCache, rankTotal + index, best);
        }
    }

    private Map<String, CreditBucket> summarizeCredits(Map<String, Requirement> requirements,
                                                       List<Map<String, Object>> selectedCourses) {
        Map<String, CreditBucket> buckets = new LinkedHashMap<>();
        requirements.forEach((type, requirement) -> buckets.put(type, new CreditBucket(requirement)));
        for (Map<String, Object> course : selectedCourses) {
            String type = asString(course.get("courseType"), "MAJOR_ELECTIVE");
            CreditBucket bucket = buckets.get(type);
            if (bucket == null) {
                bucket = buckets.get("MAJOR_ELECTIVE");
            }
            double credit = asDouble(course.get("credit"), 2);
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

    private static class PathSelection {
        private List<Map<String, Object>> courses = Collections.emptyList();
        private double remaining = Double.MAX_VALUE;
        private double overfill = Double.MAX_VALUE;
        private int rankTotal = Integer.MAX_VALUE;

        private void consider(List<Map<String, Object>> candidateCourses,
                              Map<String, Double> missing,
                              Map<String, Double> contributions,
                              int candidateRankTotal) {
            double candidateRemaining = 0;
            double candidateOverfill = 0;
            for (Map.Entry<String, Double> entry : missing.entrySet()) {
                double contribution = contributions.getOrDefault(entry.getKey(), 0.0);
                candidateRemaining += Math.max(0, entry.getValue() - contribution);
                candidateOverfill += Math.max(0, contribution - entry.getValue());
            }
            boolean better = candidateRemaining < remaining
                    || (candidateRemaining == remaining && candidateOverfill < overfill)
                    || (candidateRemaining == remaining && candidateOverfill == overfill && candidateCourses.size() < courses.size())
                    || (candidateRemaining == remaining && candidateOverfill == overfill
                    && candidateCourses.size() == courses.size() && candidateRankTotal < rankTotal);
            if (better) {
                courses = new ArrayList<>(candidateCourses);
                remaining = candidateRemaining;
                overfill = candidateOverfill;
                rankTotal = candidateRankTotal;
            }
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

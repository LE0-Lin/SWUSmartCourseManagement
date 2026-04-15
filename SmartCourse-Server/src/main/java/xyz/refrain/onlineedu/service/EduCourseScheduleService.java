package xyz.refrain.onlineedu.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.stereotype.Service;
import xyz.refrain.onlineedu.mapper.EduCourseMapper;
import xyz.refrain.onlineedu.mapper.EduCourseScheduleMapper;
import xyz.refrain.onlineedu.mapper.EduTeacherMapper;
import xyz.refrain.onlineedu.mapper.RelCourseMemberMapper;
import xyz.refrain.onlineedu.model.entity.EduCourseEntity;
import xyz.refrain.onlineedu.model.entity.EduCourseScheduleEntity;
import xyz.refrain.onlineedu.model.entity.EduTeacherEntity;
import xyz.refrain.onlineedu.model.entity.RelCourseMemberEntity;
import xyz.refrain.onlineedu.model.vo.R;
import xyz.refrain.onlineedu.utils.RUtils;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 课表安排 服务
 *
 * @author SWU
 */
@Service
@Slf4j
public class EduCourseScheduleService {

    @Resource
    private EduCourseScheduleMapper eduCourseScheduleMapper;

    @Resource
    private RelCourseMemberMapper relCourseMemberMapper;

    @Resource
    private EduCourseMapper eduCourseMapper;

    @Resource
    private EduTeacherMapper eduTeacherMapper;

    /**
     * 获取课程安排
     */
    public R getScheduleByCourseId(int courseId) {
        List<EduCourseScheduleEntity> list = eduCourseScheduleMapper.selectList(
                Wrappers.lambdaQuery(EduCourseScheduleEntity.class)
                        .eq(EduCourseScheduleEntity::getCourseId, courseId)
                        .orderByAsc(EduCourseScheduleEntity::getDayOfWeek)
                        .orderByAsc(EduCourseScheduleEntity::getSectionStart)
        );
        return RUtils.success("课程表安排", list);
    }

    /**
     * 保存或更新安排
     */
    public R saveOrUpdateSchedule(EduCourseScheduleEntity entity) {
        int i;
        if (entity.getId() != null) {
            i = eduCourseScheduleMapper.updateById(entity);
        } else {
            i = eduCourseScheduleMapper.insert(entity);
        }
        return RUtils.commonFailOrNot(i, "保存课表安排");
    }

    /**
     * 删除安排
     */
    public R deleteSchedule(int id) {
        int i = eduCourseScheduleMapper.deleteById(id);
        return RUtils.commonFailOrNot(i, "删除课表安排");
    }

    public R getStudentSchedule(int memberId) {
        List<RelCourseMemberEntity> relations = relCourseMemberMapper.selectList(
                Wrappers.lambdaQuery(RelCourseMemberEntity.class)
                        .select(RelCourseMemberEntity::getCourseId)
                        .eq(RelCourseMemberEntity::getMemberId, memberId)
        );
        if (CollectionUtils.isEmpty(relations)) {
            return RUtils.success("我的课表", Collections.emptyList());
        }
        Set<Integer> courseIds = relations.stream()
                .map(RelCourseMemberEntity::getCourseId)
                .collect(Collectors.toSet());
        return RUtils.success("我的课表", buildScheduleRows(courseIds));
    }

    public R getTeacherSchedule(int teacherId) {
        List<EduCourseEntity> courses = eduCourseMapper.selectList(
                Wrappers.lambdaQuery(EduCourseEntity.class)
                        .select(EduCourseEntity::getId)
                        .eq(EduCourseEntity::getTeacherId, teacherId)
        );
        if (CollectionUtils.isEmpty(courses)) {
            return RUtils.success("我的课表", Collections.emptyList());
        }
        Set<Integer> courseIds = courses.stream()
                .map(EduCourseEntity::getId)
                .collect(Collectors.toSet());
        return RUtils.success("我的课表", buildScheduleRows(courseIds));
    }

    private List<Map<String, Object>> buildScheduleRows(Set<Integer> courseIds) {
        if (CollectionUtils.isEmpty(courseIds)) {
            return Collections.emptyList();
        }
        List<EduCourseScheduleEntity> schedules = eduCourseScheduleMapper.selectList(
                Wrappers.lambdaQuery(EduCourseScheduleEntity.class)
                        .in(EduCourseScheduleEntity::getCourseId, courseIds)
                        .orderByAsc(EduCourseScheduleEntity::getDayOfWeek)
                        .orderByAsc(EduCourseScheduleEntity::getSectionStart)
        );
        if (CollectionUtils.isEmpty(schedules)) {
            return Collections.emptyList();
        }
        List<EduCourseEntity> courses = eduCourseMapper.selectList(
                Wrappers.lambdaQuery(EduCourseEntity.class)
                        .in(EduCourseEntity::getId, courseIds)
        );
        Map<Integer, EduCourseEntity> courseMap = courses.stream()
                .collect(Collectors.toMap(EduCourseEntity::getId, e -> e));
        Set<Integer> teacherIds = courses.stream()
                .map(EduCourseEntity::getTeacherId)
                .collect(Collectors.toSet());
        List<EduTeacherEntity> teachers = CollectionUtils.isEmpty(teacherIds) ? Collections.emptyList()
                : eduTeacherMapper.selectList(
                Wrappers.lambdaQuery(EduTeacherEntity.class)
                        .in(EduTeacherEntity::getId, teacherIds)
        );
        Map<Integer, String> teacherMap = teachers.stream()
                .collect(Collectors.toMap(EduTeacherEntity::getId, EduTeacherEntity::getName));
        return schedules.stream().map(item -> {
            EduCourseEntity course = courseMap.get(item.getCourseId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", item.getId());
            row.put("courseId", item.getCourseId());
            row.put("title", course != null ? course.getTitle() : "");
            row.put("teacherName", course != null ? teacherMap.getOrDefault(course.getTeacherId(), "") : "");
            row.put("dayOfWeek", item.getDayOfWeek());
            row.put("sectionStart", item.getSectionStart());
            row.put("sectionEnd", item.getSectionEnd());
            row.put("location", item.getLocation());
            return row;
        }).collect(Collectors.toList());
    }
}

package xyz.refrain.onlineedu.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import xyz.refrain.onlineedu.mapper.EduCourseMapper;
import xyz.refrain.onlineedu.mapper.EduGradeMapper;
import xyz.refrain.onlineedu.mapper.RelCourseMemberMapper;
import xyz.refrain.onlineedu.mapper.UctrMemberMapper;
import xyz.refrain.onlineedu.model.entity.EduCourseEntity;
import xyz.refrain.onlineedu.model.entity.EduGradeEntity;
import xyz.refrain.onlineedu.model.entity.RelCourseMemberEntity;
import xyz.refrain.onlineedu.model.entity.UctrMemberEntity;
import xyz.refrain.onlineedu.model.vo.R;
import xyz.refrain.onlineedu.utils.RUtils;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 成绩 服务
 *
 * @author SWU
 */
@Service
@Slf4j
public class EduGradeService {

    @Resource
    private EduGradeMapper eduGradeMapper;

    @Resource
    private RelCourseMemberMapper relCourseMemberMapper;

    @Resource
    private UctrMemberMapper uctrMemberMapper;

    @Resource
    private EduCourseMapper eduCourseMapper;

    /**
     * 获取某课程的所有成绩（老师用）
     */
    public R getGradesByCourseId(int courseId) {
        List<RelCourseMemberEntity> members = relCourseMemberMapper.selectList(
                Wrappers.lambdaQuery(RelCourseMemberEntity.class)
                        .eq(RelCourseMemberEntity::getCourseId, courseId)
        );
        if (members.isEmpty()) {
            return RUtils.success("课程成绩列表", Collections.emptyList());
        }
        Set<Integer> memberIds = members.stream()
                .map(RelCourseMemberEntity::getMemberId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<UctrMemberEntity> memberEntities = uctrMemberMapper.selectList(
                Wrappers.lambdaQuery(UctrMemberEntity.class)
                        .in(UctrMemberEntity::getId, memberIds)
        );
        Map<Integer, UctrMemberEntity> memberMap = memberEntities.stream()
                .collect(Collectors.toMap(UctrMemberEntity::getId, e -> e));
        List<EduGradeEntity> gradeEntities = eduGradeMapper.selectList(
                Wrappers.lambdaQuery(EduGradeEntity.class)
                        .eq(EduGradeEntity::getCourseId, courseId)
        );
        Map<Integer, EduGradeEntity> gradeMap = gradeEntities.stream()
                .collect(Collectors.toMap(EduGradeEntity::getMemberId, e -> e, (left, right) -> right));
        List<Map<String, Object>> result = members.stream().map(item -> {
            UctrMemberEntity member = memberMap.get(item.getMemberId());
            EduGradeEntity grade = gradeMap.get(item.getMemberId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("memberId", item.getMemberId());
            row.put("nickname", Objects.nonNull(member) ? member.getNickname() : "");
            row.put("mobile", Objects.nonNull(member) ? member.getMobile() : "");
            row.put("email", Objects.nonNull(member) ? member.getEmail() : "");
            row.put("avatar", Objects.nonNull(member) ? member.getAvatar() : "");
            row.put("score", Objects.nonNull(grade) ? grade.getScore() : null);
            row.put("updateTime", Objects.nonNull(grade) ? grade.getUpdateTime() : item.getUpdateTime());
            return row;
        }).collect(Collectors.toList());
        return RUtils.success("课程成绩列表", result);
    }

    /**
     * 获取学生在某课程的成绩（学生用）
     */
    public R getStudentGrade(int courseId, int memberId) {
        EduGradeEntity entity = eduGradeMapper.selectOne(
                Wrappers.lambdaQuery(EduGradeEntity.class)
                        .eq(EduGradeEntity::getCourseId, courseId)
                        .eq(EduGradeEntity::getMemberId, memberId)
        );
        return RUtils.success("我的成绩", entity);
    }

    /**
     * 保存或更新成绩
     */
    public R saveOrUpdateGrade(EduGradeEntity entity) {
        EduGradeEntity exist = eduGradeMapper.selectOne(
                Wrappers.lambdaQuery(EduGradeEntity.class)
                        .eq(EduGradeEntity::getCourseId, entity.getCourseId())
                        .eq(EduGradeEntity::getMemberId, entity.getMemberId())
        );
        int i;
        if (exist != null) {
            entity.setId(exist.getId());
            i = eduGradeMapper.updateById(entity);
        } else {
            i = eduGradeMapper.insert(entity);
        }
        return RUtils.commonFailOrNot(i, "保存成绩");
    }

    /**
     * 获取成绩分布统计
     */
    public R getGradeDistribution(int courseId) {
        List<EduGradeEntity> grades = eduGradeMapper.selectList(
                Wrappers.lambdaQuery(EduGradeEntity.class)
                        .eq(EduGradeEntity::getCourseId, courseId)
        );
        
        int[] distribution = new int[5]; // <60, 60-70, 70-80, 80-90, 90-100
        for (EduGradeEntity g : grades) {
            double score = g.getScore();
            if (score < 60) distribution[0]++;
            else if (score < 70) distribution[1]++;
            else if (score < 80) distribution[2]++;
            else if (score < 90) distribution[3]++;
            else distribution[4]++;
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("labels", new String[]{"不及格(<60)", "及格(60-70)", "中等(70-80)", "良好(80-90)", "优秀(90-100)"});
        result.put("counts", distribution);
        
        return RUtils.success("成绩分布统计", result);
    }

    public R getTranscript(int memberId) {
        List<RelCourseMemberEntity> relations = relCourseMemberMapper.selectList(
                Wrappers.lambdaQuery(RelCourseMemberEntity.class)
                        .eq(RelCourseMemberEntity::getMemberId, memberId)
        );
        if (relations.isEmpty()) {
            return RUtils.success("成绩单列表", Collections.emptyList());
        }
        Set<Integer> courseIds = relations.stream()
                .map(RelCourseMemberEntity::getCourseId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<EduCourseEntity> courseEntities = eduCourseMapper.selectList(
                Wrappers.lambdaQuery(EduCourseEntity.class)
                        .in(EduCourseEntity::getId, courseIds)
        );
        Map<Integer, EduCourseEntity> courseMap = courseEntities.stream()
                .collect(Collectors.toMap(EduCourseEntity::getId, e -> e));
        List<EduGradeEntity> gradeEntities = eduGradeMapper.selectList(
                Wrappers.lambdaQuery(EduGradeEntity.class)
                        .eq(EduGradeEntity::getMemberId, memberId)
                        .in(EduGradeEntity::getCourseId, courseIds)
        );
        Map<Integer, EduGradeEntity> gradeMap = gradeEntities.stream()
                .collect(Collectors.toMap(EduGradeEntity::getCourseId, e -> e, (left, right) -> right));
        List<Map<String, Object>> result = relations.stream().map(item -> {
            EduCourseEntity course = courseMap.get(item.getCourseId());
            EduGradeEntity grade = gradeMap.get(item.getCourseId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("courseId", item.getCourseId());
            row.put("courseName", Objects.nonNull(course) ? course.getTitle() : "");
            row.put("score", Objects.nonNull(grade) ? grade.getScore() : null);
            row.put("status", Objects.nonNull(grade) && Objects.nonNull(grade.getScore()) ? "graded" : "pending");
            row.put("updateTime", Objects.nonNull(grade) ? grade.getUpdateTime() : item.getUpdateTime());
            return row;
        }).collect(Collectors.toList());
        return RUtils.success("成绩单列表", result);
    }
}

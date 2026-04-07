package xyz.refrain.onlineedu.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import xyz.refrain.onlineedu.mapper.EduGradeMapper;
import xyz.refrain.onlineedu.model.entity.EduGradeEntity;
import xyz.refrain.onlineedu.model.vo.R;
import xyz.refrain.onlineedu.utils.RUtils;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 成绩 服务
 *
 * @author Gemini CLI
 */
@Service
@Slf4j
public class EduGradeService {

    @Resource
    private EduGradeMapper eduGradeMapper;

    /**
     * 获取某课程的所有成绩（老师用）
     */
    public R getGradesByCourseId(int courseId) {
        List<EduGradeEntity> list = eduGradeMapper.selectList(
                Wrappers.lambdaQuery(EduGradeEntity.class)
                        .eq(EduGradeEntity::getCourseId, courseId)
        );
        return RUtils.success("课程成绩列表", list);
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
}

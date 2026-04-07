package xyz.refrain.onlineedu.controller.teacher;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import xyz.refrain.onlineedu.model.entity.EduGradeEntity;
import xyz.refrain.onlineedu.model.securtiy.EduTeacherDetail;
import xyz.refrain.onlineedu.model.vo.R;
import xyz.refrain.onlineedu.service.EduCourseService;
import xyz.refrain.onlineedu.service.EduGradeService;
import xyz.refrain.onlineedu.utils.IPUtils;
import xyz.refrain.onlineedu.utils.RUtils;
import xyz.refrain.onlineedu.utils.SessionUtils;

import javax.validation.constraints.Min;
import java.util.Set;

/**
 * 讲师端成绩控制器
 *
 * @author Gemini CLI
 */
@Validated
@RestController("TeacherEduGradeController")
@RequestMapping("/api/teacher/grade")
@Api(value = "讲师端成绩控制器", tags = {"讲师端成绩接口"})
public class EduGradeController {

    @Autowired
    private EduGradeService eduGradeService;

    @Autowired
    private EduCourseService eduCourseService;

    @GetMapping("/list/{courseId}")
    @ApiOperation("获取某课程的所有成绩")
    public R list(@PathVariable("courseId") @Min(1) Integer courseId) {
        if (!isTeachersCourse(courseId)) {
            return RUtils.fail("无权操作此课程");
        }
        return eduGradeService.getGradesByCourseId(courseId);
    }

    @PostMapping("/save")
    @ApiOperation("保存或更新成绩")
    public R save(@RequestBody EduGradeEntity entity) {
        if (entity.getCourseId() == null || !isTeachersCourse(entity.getCourseId())) {
            return RUtils.fail("无权操作此课程");
        }
        return eduGradeService.saveOrUpdateGrade(entity);
    }

    @GetMapping("/distribution/{courseId}")
    @ApiOperation("获取成绩分布统计")
    public R distribution(@PathVariable("courseId") @Min(1) Integer courseId) {
        if (!isTeachersCourse(courseId)) {
            return RUtils.fail("无权操作此课程");
        }
        return eduGradeService.getGradeDistribution(courseId);
    }

    /**
     * 判断是否是讲师的课程
     */
    private boolean isTeachersCourse(int courseId) {
        EduTeacherDetail teacher = SessionUtils.getTeacher(IPUtils.getRequest());
        Set<Integer> ids = eduCourseService.getTeacherCourseIds(teacher.getId());
        return ids.contains(courseId);
    }
}

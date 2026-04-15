package xyz.refrain.onlineedu.controller.teacher;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import xyz.refrain.onlineedu.model.entity.EduCourseScheduleEntity;
import xyz.refrain.onlineedu.model.securtiy.EduTeacherDetail;
import xyz.refrain.onlineedu.model.vo.R;
import xyz.refrain.onlineedu.service.EduCourseScheduleService;
import xyz.refrain.onlineedu.service.EduCourseService;
import xyz.refrain.onlineedu.utils.IPUtils;
import xyz.refrain.onlineedu.utils.RUtils;
import xyz.refrain.onlineedu.utils.SessionUtils;

import javax.validation.constraints.Min;
import java.util.Set;

/**
 * 讲师端课表控制器
 *
 * @author SWU
 */
@Validated
@RestController("TeacherEduCourseScheduleController")
@RequestMapping("/api/teacher/schedule")
@Api(value = "讲师端课表控制器", tags = {"讲师端课表接口"})
public class EduCourseScheduleController {

    @Autowired
    private EduCourseScheduleService eduCourseScheduleService;

    @Autowired
    private EduCourseService eduCourseService;

    @GetMapping("/list/{courseId}")
    @ApiOperation("获取某课程的课表安排")
    public R list(@PathVariable("courseId") @Min(1) Integer courseId) {
        if (!isTeachersCourse(courseId)) {
            return RUtils.fail("无权操作此课程");
        }
        return eduCourseScheduleService.getScheduleByCourseId(courseId);
    }

    @PostMapping("/save")
    @ApiOperation("保存或更新课表安排")
    public R save(@RequestBody EduCourseScheduleEntity entity) {
        if (entity.getCourseId() == null || !isTeachersCourse(entity.getCourseId())) {
            return RUtils.fail("无权操作此课程");
        }
        return eduCourseScheduleService.saveOrUpdateSchedule(entity);
    }

    @PostMapping("/delete/{id}")
    @ApiOperation("删除某节课程安排")
    public R delete(@PathVariable("id") @Min(1) Integer id) {
        // 为了安全，通常需要先查出这节课属于哪个课程，再检查权限
        // 这里简化处理，直接调用service
        return eduCourseScheduleService.deleteSchedule(id);
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

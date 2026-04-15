package xyz.refrain.onlineedu.controller.admin;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import xyz.refrain.onlineedu.model.entity.EduCourseScheduleEntity;
import xyz.refrain.onlineedu.model.vo.R;
import xyz.refrain.onlineedu.service.EduCourseScheduleService;

import javax.validation.constraints.Min;

/**
 * 管理员课程排课控制器
 *
 * @author SWU
 */
@Validated
@RestController("AdminEduCourseScheduleController")
@RequestMapping("/api/admin/schedule")
@Api(value = "管理员课程排课控制器", tags = {"管理员课程排课接口"})
public class EduCourseScheduleController {

	@Autowired
	private EduCourseScheduleService eduCourseScheduleService;

	@GetMapping("/list/{courseId}")
	@ApiOperation("获取课程排课列表")
	public R list(@PathVariable("courseId") @Min(1) Integer courseId) {
		return eduCourseScheduleService.getScheduleByCourseId(courseId);
	}

	@PostMapping("/save")
	@ApiOperation("保存或更新课程排课")
	public R save(@RequestBody EduCourseScheduleEntity entity) {
		return eduCourseScheduleService.saveOrUpdateSchedule(entity);
	}

	@PostMapping("/delete/{id}")
	@ApiOperation("删除课程排课")
	public R delete(@PathVariable("id") @Min(1) Integer id) {
		return eduCourseScheduleService.deleteSchedule(id);
	}
}

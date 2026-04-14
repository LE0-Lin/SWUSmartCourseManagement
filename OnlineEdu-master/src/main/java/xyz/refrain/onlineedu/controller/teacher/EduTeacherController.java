package xyz.refrain.onlineedu.controller.teacher;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import xyz.refrain.onlineedu.annotation.AccessLimit;
import xyz.refrain.onlineedu.annotation.TimeCost;
import xyz.refrain.onlineedu.model.params.LoginParam;
import xyz.refrain.onlineedu.model.params.UpdatePasswordParam;
import xyz.refrain.onlineedu.model.securtiy.EduTeacherDetail;
import xyz.refrain.onlineedu.model.vo.R;
import xyz.refrain.onlineedu.service.EduTeacherService;
import xyz.refrain.onlineedu.utils.IPUtils;
import xyz.refrain.onlineedu.utils.RUtils;
import xyz.refrain.onlineedu.utils.SessionUtils;

import javax.validation.Valid;
import java.io.IOException;

/**
 * 讲师端讲师控制器
 *
 * @author SWU
 */
@Validated
@RestController("TeacherEduTeacherController")
@RequestMapping("/api/teacher/user")
@Api(value = "讲师端讲师控制器", tags = {"讲师端讲师接口"})
public class EduTeacherController {

	@Autowired
	private EduTeacherService eduTeacherService;

	@TimeCost
	@AccessLimit(maxCount = 3, seconds = 300)
	@PostMapping("/login")
	@ApiOperation("登录")
	public R login(@RequestBody @Valid LoginParam param) {
		return eduTeacherService.login(param);
	}

	@PostMapping("/logout")
	@ApiOperation("登出")
	public R logout() {
		return eduTeacherService.logout();
	}

	@GetMapping("/info")
	@ApiOperation("获取登录用户信息")
	public R info() {
		return eduTeacherService.info();
	}

	@PostMapping("/update/password")
	@ApiOperation("修改密码")
	public R updatePassword(@RequestBody @Valid UpdatePasswordParam param) {
		return eduTeacherService.updatePassword(param);
	}

	@PostMapping("/update/avatar")
	@ApiOperation("修改头像")
	public R updateAvatar(@RequestPart("file") MultipartFile file) throws IOException {
		EduTeacherDetail detail = SessionUtils.getTeacher(IPUtils.getRequest());
		R r = eduTeacherService.updateAvatar(detail.getId(), file);
		if (r.getStatus() == 200) {
			String newAvatarUrl = (String) r.getData();
			detail.setAvatar(newAvatarUrl);
			SessionUtils.saveTeacher(detail);
		}
		return r;
	}

}

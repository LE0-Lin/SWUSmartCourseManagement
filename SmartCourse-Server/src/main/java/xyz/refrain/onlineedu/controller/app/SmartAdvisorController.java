package xyz.refrain.onlineedu.controller.app;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.refrain.onlineedu.constant.RS;
import xyz.refrain.onlineedu.model.securtiy.UctrMemberDetail;
import xyz.refrain.onlineedu.model.vo.R;
import xyz.refrain.onlineedu.service.SmartAdvisorService;
import xyz.refrain.onlineedu.utils.SessionUtils;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Min;
import java.util.Objects;

@Validated
@RestController("AppSmartAdvisorController")
@RequestMapping("/api/app/smart/advisor")
@Api(value = "学生智能选课与毕业预警", tags = {"学生智能选课与毕业预警接口"})
public class SmartAdvisorController {

    @Autowired
    private SmartAdvisorService smartAdvisorService;

    @GetMapping("/dashboard")
    @ApiOperation("获取智能毕业路径驾驶舱")
    public R dashboard(HttpServletRequest request) {
        UctrMemberDetail member = SessionUtils.getMember(request);
        if (Objects.isNull(member)) {
            return new R(RS.NOT_LOGIN.status(), "请登录后再操作");
        }
        return smartAdvisorService.getStudentDashboard(member.getId());
    }

    @GetMapping("/diagnose/course/{courseId}")
    @ApiOperation("诊断课程是否适合当前学生选择")
    public R diagnoseCourse(@PathVariable("courseId") @Min(1) Integer courseId, HttpServletRequest request) {
        UctrMemberDetail member = SessionUtils.getMember(request);
        if (Objects.isNull(member)) {
            return new R(RS.NOT_LOGIN.status(), "请登录后再操作");
        }
        return smartAdvisorService.diagnoseCourse(member.getId(), courseId);
    }
}

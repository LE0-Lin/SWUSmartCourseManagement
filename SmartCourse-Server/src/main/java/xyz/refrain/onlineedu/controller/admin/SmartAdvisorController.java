package xyz.refrain.onlineedu.controller.admin;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.refrain.onlineedu.model.vo.R;
import xyz.refrain.onlineedu.service.SmartAdvisorService;

@RestController("AdminSmartAdvisorController")
@RequestMapping("/api/admin/smart/advisor")
@Api(value = "管理员智能毕业预警", tags = {"管理员智能毕业预警接口"})
public class SmartAdvisorController {

    @Autowired
    private SmartAdvisorService smartAdvisorService;

    @GetMapping("/overview")
    @ApiOperation("获取全校毕业风险总览")
    public R overview() {
        return smartAdvisorService.getAdminOverview();
    }
}

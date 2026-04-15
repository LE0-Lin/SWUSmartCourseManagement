package xyz.refrain.onlineedu.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import xyz.refrain.onlineedu.mapper.EduCourseScheduleMapper;
import xyz.refrain.onlineedu.model.entity.EduCourseScheduleEntity;
import xyz.refrain.onlineedu.model.vo.R;
import xyz.refrain.onlineedu.utils.RUtils;

import javax.annotation.Resource;
import java.util.List;

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
}

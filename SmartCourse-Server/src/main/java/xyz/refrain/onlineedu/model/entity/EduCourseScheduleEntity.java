package xyz.refrain.onlineedu.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 课程表安排
 * </p>
 *
 * @author SWU
 * @since 2026-04-07
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("edu_course_schedule")
@ApiModel(value = "EduCourseScheduleEntity对象", description = "课程表安排")
public class EduCourseScheduleEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "课表ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "课程ID")
    @TableField("course_id")
    private Integer courseId;

    @ApiModelProperty(value = "周几(1-7)")
    @TableField("day_of_week")
    private Integer dayOfWeek;

    @ApiModelProperty(value = "开始节次(1-12)")
    @TableField("section_start")
    private Integer sectionStart;

    @ApiModelProperty(value = "结束节次(1-12)")
    @TableField("section_end")
    private Integer sectionEnd;

    @ApiModelProperty(value = "开始周(1-21)")
    @TableField("start_week")
    private Integer startWeek;

    @ApiModelProperty(value = "结束周(1-21)")
    @TableField("end_week")
    private Integer endWeek;

    @ApiModelProperty(value = "上课地点")
    @TableField("location")
    private String location;

    @ApiModelProperty(value = "更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @ApiModelProperty(value = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

}

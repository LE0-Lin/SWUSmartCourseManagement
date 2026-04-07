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
 * 学生成绩表
 * </p>
 *
 * @author Gemini CLI
 * @since 2026-04-07
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("edu_grade")
@ApiModel(value = "EduGradeEntity对象", description = "学生成绩")
public class EduGradeEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "成绩ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "课程ID")
    @TableField("course_id")
    private Integer courseId;

    @ApiModelProperty(value = "学员ID")
    @TableField("member_id")
    private Integer memberId;

    @ApiModelProperty(value = "分数")
    @TableField("score")
    private Double score;

    @ApiModelProperty(value = "更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @ApiModelProperty(value = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

}

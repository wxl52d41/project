package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("STUDENT")
public class Student {

    /**
     * ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String studentName;

    private Integer age;

    private Integer sex;

    /**
     * 创建人
     */
    @TableField("CREATE_BY")
    private String createBy;

    /**
     * 创建人姓名
     */
    @TableField("CREATE_BY_NAME")
    private String createByName;

    /**
     * 创建时间
     */
    @TableField("CREATE_TIME")
    private Date createTime;

    /**
     * 更新人
     */
    @TableField("UPDATE_BY")
    private String updateBy;

    /**
     * 更新人姓名
     */
    @TableField("UPDATE_BY_NAME")
    private String updateByName;

    /**
     * 更新时间
     */
    @TableField("UPDATE_TIME")
    private Date updateTime;

    /**
     * 0：否，1：是
     */
    @TableField("IS_DELETE")
    private Integer isDelete;

}

package com.wxl52d41.validator.vo;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

/**
 * @author xlwang55
 * @date 2022/2/22 10:38
 */
@Data
public class TestVO {

    private String id;

    @NotEmpty(message = "标题必填")
    @Size(max = 10,message = "标题不能超过10字")
    private String title;

    @NotEmpty(message = "内容必填")
    private String content;

    @NotEmpty(message = "接收人必填")
    private String receiver;

}

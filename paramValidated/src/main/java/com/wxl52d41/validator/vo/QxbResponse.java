package com.wxl52d41.validator.vo;

import lombok.Data;

/**
 * <p>
 * <code>QxbResponse</code>
 * </p>
 * Description:
 * Qxb内部接口统一返回
 * @author jianzh5
 * @date 2020/7/15 9:57
 */
@Data
public class QxbResponse<T> {
    private boolean result;
    private String message;
    private T content;
}

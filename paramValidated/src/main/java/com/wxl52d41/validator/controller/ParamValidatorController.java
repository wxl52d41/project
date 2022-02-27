package com.wxl52d41.validator.controller;


import com.wxl52d41.validator.validator.InsertValidator;
import com.wxl52d41.validator.vo.TestVO;
import com.wxl52d41.validator.vo.User;
import lombok.NonNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author xlwang55
 * @since DATE{TIME}
 */
@RestController
@RequestMapping("/test")
public class ParamValidatorController {
    //
    @PostMapping("emailAlert")
    public void emailAlert(@NonNull String title, @NonNull String content,
                           @NonNull String receiver, @NonNull String cc) {
        System.out.println("title = " + title);
        //如果某个字段是否为空,并不是校验内容是否为空
        //{
        //    "success": false,
        //    "message": "title is marked non-null but is null",
        //    "code": 500,
        //    "result": null,
        //    "timestamp": 1645779293625
        //}
    }

    @PostMapping("update")
    public void update(@RequestBody @Validated TestVO testVO) {
        //校验字段和内容是否为空
        System.out.println("testVO = " + testVO);

        // 入参
        // {
        //    "id": "",
        //    "content": "",
        //    "receiver": "demoData"
        //}

        //结果
        //{
        //    "success": false,
        //    "message": "内容必填; 标题必填",
        //    "code": 400,
        //    "result": null,
        //    "timestamp": 1645779744984
        //}
    }


}
